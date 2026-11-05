package logisticspipes.transport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.order.IDistanceTracker;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.item.ItemRoutingInformation;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.SlidingWindowBitSet;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * Something moving through a pipe: where it is along the pipe, how fast it goes, where it came in
 * and where it goes out.
 *
 * <p>The server owns the real item, with its routing, and tells the clients watching the pipe
 * about it by id: what it is once, and where it is every time it enters a pipe. Each client keeps
 * its own copy and moves it between those updates.</p>
 */
public abstract class LPTravelingItem {

    /**
     * The server items by id, so a client that missed what one of them is can ask for it. Weak:
     * an item leaves once nothing else holds it.
     */
    public static final Map<Integer, LPTravelingItemServer> serverList = new MapMaker().weakValues().makeMap();

    /**
     * The client copies by id, so the server's updates can find them. Weak, like {@link #serverList}.
     */
    public static final Map<Integer, LPTravelingItemClient> clientList = new MapMaker().weakValues().makeMap();

    /**
     * Client items held for a few more ticks, so they are still in {@link #clientList} when the
     * position update that puts them in a pipe comes.
     */
    public static final Map<LPTravelingItemClient, Integer> forceKeep = new HashMap<>();

    /**
     * The ids whose content the clients have already been sent.
     */
    public static final SlidingWindowBitSet clientSideKnownIDs = new SlidingWindowBitSet(20);

    /**
     * Saves a direction by name, and still reads the number LP saved it as before, so items left
     * in pipes in an older world keep their way.
     */
    // TODO: 26.3: use Direction.CODEC alone and drop the conversion from the old numbers.
    private static final Codec<Direction> DIRECTION_CODEC =
        Codec.withAlternative(Direction.CODEC, Direction.LEGACY_ID_CODEC);

    private static int nextFreeId = 0;

    @Getter
    protected int id;
    @Getter
    @Setter
    protected float speed = 0.01F;
    @Getter
    @Setter
    protected @Nullable BlockEntity container;
    /**
     * How far along the pipe the item is, from 0 at the input to the pipe's length at the output.
     */
    @Getter
    @Setter
    protected float position = 0;
    @Getter
    protected float yaw = 0;
    /**
     * The direction the item travels in when it comes in, so the side it came from is the opposite.
     */
    public @Nullable Direction input = null;
    /**
     * The side the item leaves by, or null while it has none: on the server when it has nowhere to
     * go, on a client until the server says.
     */
    public @Nullable Direction output = null;

    /**
     * The tick the item last moved in, so a pipe that ticks after the one that handed it over does
     * not move it a second time in the same tick.
     */
    int lastTicked = 0;

    protected LPTravelingItem() {
        id = ++nextFreeId;
    }

    protected LPTravelingItem(int id) {
        this.id = id;
    }

    protected LPTravelingItem(int id, float position, @Nullable Direction input, @Nullable Direction output,
        float yaw) {
        this.id = id;
        this.position = position;
        this.input = input;
        this.output = output;
        this.yaw = yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw % 360;
    }

    /**
     * What the item is, or null for a client copy the server has not told yet.
     */
    public abstract @Nullable ItemIdentifierStack getItemIdentifierStack();

    /**
     * Whether nothing is left in the item, or a client copy has not been told what it is yet.
     */
    public boolean isCorrupted() {
        ItemIdentifierStack stack = getItemIdentifierStack();
        return stack == null || stack.getStackSize() <= 0;
    }

    /**
     * A copy to draw somewhere else than the item is, such as already in the next pipe.
     */
    public abstract LPTravelingItem renderCopy();

    /**
     * A client's copy of an item: what it is and where it goes come from the server.
     */
    public static final class LPTravelingItemClient extends LPTravelingItem {

        /**
         * Null until the server says what the item is.
         */
        @Setter
        private @Nullable ItemIdentifierStack item;

        public LPTravelingItemClient(int id, float position, @Nullable Direction input,
            @Nullable Direction output, float yaw) {
            super(id, position, input, output, yaw);
        }

        public LPTravelingItemClient(int id, ItemIdentifierStack stack) {
            super(id);
            item = stack;
        }

        @Override
        public @Nullable ItemIdentifierStack getItemIdentifierStack() {
            return item;
        }

        public void updateInformation(@Nullable Direction input, @Nullable Direction output, float speed,
            float position, float yaw) {
            this.input = input;
            this.output = output;
            this.speed = speed;
            this.position = position;
            this.yaw = yaw;
        }

        @Override
        public LPTravelingItem renderCopy() {
            LPTravelingItemClient copy = new LPTravelingItemClient(id, position, input, output, yaw);
            copy.speed = speed;
            copy.item = item != null ? new ItemIdentifierStack(item) : null;
            copy.container = container;
            return copy;
        }
    }

    /**
     * The real item, with where it is headed.
     */
    public static final class LPTravelingItemServer extends LPTravelingItem implements IRoutedItem {

        @Getter
        private ItemRoutingInformation info;

        public LPTravelingItemServer(ItemStack stack) {
            this(ItemIdentifierStack.getFromStack(stack));
        }

        public LPTravelingItemServer(ItemIdentifierStack stack) {
            this(new ItemRoutingInformation());
            info.setItem(stack);
        }

        public LPTravelingItemServer(ItemRoutingInformation info) {
            this.info = info;
            serverList.put(id, this);
        }

        public LPTravelingItemServer(ValueInput data) {
            this(new ItemRoutingInformation());
            deserialize(data);
        }

        /**
         * Gives the item a new id, so the clients ask again what it is: the clients only learn
         * that once per id.
         */
        public void newId() {
            id = ++nextFreeId;
            serverList.put(id, this);
        }

        @Override
        public ItemIdentifierStack getItemIdentifierStack() {
            return info.getItem();
        }

        public void setInformation(ItemRoutingInformation info) {
            this.info = info;
        }

        @Override
        public void deserialize(ValueInput input) {
            setPosition(input.getFloatOr("position", 0.0f));
            setSpeed(input.getFloatOr("speed", 0.0f));
            this.input = input.read("input", DIRECTION_CODEC).orElse(null);
            this.output = input.read("output", DIRECTION_CODEC).orElse(null);
            info.deserialize(input);
        }

        @Override
        public void serialize(ValueOutput output) {
            output.putFloat("position", getPosition());
            output.putFloat("speed", getSpeed());
            output.storeNullable("input", DIRECTION_CODEC, this.input);
            output.storeNullable("output", DIRECTION_CODEC, this.output);
            info.serialize(output);
        }

        /**
         * The item as an entity dropped out of its pipe, flying on the way it was going, or null if
         * there is nothing to drop: an empty item, or a fluid container, which only exists in pipes.
         * Either way the destination is told the item is lost.
         */
        public @Nullable ItemEntity toEntityItem() {
            BlockEntity pipe = Objects.requireNonNull(container, "Dropping an item that is in no pipe");
            Level level = Objects.requireNonNull(pipe.getLevel(), "Dropping an item from a pipe outside a level");
            if (level.isClientSide() || getItemIdentifierStack().getStackSize() <= 0) {
                return null;
            }
            if (getItemIdentifierStack().makeNormalStack().getItem() instanceof LogisticsFluidContainer) {
                itemWasLost();
                return null;
            }

            Direction exit = output != null ? output : input;
            Vec3 position = Vec3.atLowerCornerOf(pipe.getBlockPos()).add(0.5, 0.375, 0.5);
            Vec3 motion = Vec3.ZERO;
            if (exit != null) {
                double offset = switch (exit) {
                    case DOWN -> 0.5;
                    case UP -> 0.75;
                    case NORTH, SOUTH, WEST, EAST -> 0.625;
                };
                position = position.relative(exit, offset);
                motion = motion.relative(exit, getSpeed() * 2.0);
            }

            ItemEntity entity = new ItemEntity(level, position.x, position.y, position.z,
                getItemIdentifierStack().makeNormalStack());
            // Uniformly distributed in -0.005 .. 0.01, to favor smaller values.
            float spread = level.getRandom().nextFloat() * 0.015F - 0.005F;
            entity.setDeltaMovement(
                level.getRandom().nextGaussian() * spread + motion.x,
                level.getRandom().nextGaussian() * spread + motion.y,
                level.getRandom().nextGaussian() * spread + motion.z);
            itemWasLost();
            return entity;
        }

        @Override
        public void clearDestination() {
            if (info.destinationint >= 0) {
                itemWasLost();
                info.jamlist.add(info.destinationint);
            }
            // The buffer counter and the jam list stay.
            info.destinationint = -1;
            info.destinationUUID = null;
            info.doNotBuffer = false;
            info.arrived = false;
            info.transportMode = TransportMode.Unknown;
            info.targetInfo = null;
        }

        /**
         * Tells the destination the item will not arrive, so it can order it again.
         */
        private void itemWasLost() {
            if (container != null && container.getLevel() != null && container.getLevel().isClientSide()) {
                return;
            }
            IRouter destinationRouter = RouterManager.get().getRouter(info.destinationint);
            if (destinationRouter == null || destinationRouter.getPipe() == null) {
                return;
            }
            CoreRoutedPipe destination = destinationRouter.getPipe();
            destination.notifyOfReroute(info);
            if (destination instanceof IRequireReliableTransport reliable) {
                reliable.itemLost(new ItemIdentifierStack(info.getItem()), info.targetInfo);
            }
            if (destination instanceof IRequireReliableFluidTransport reliable
                && info.getItem().getItem().isFluidContainer()) {
                FluidIdentifierStack liquid = LogisticsFluidContainer.getFluid(info.getItem());
                reliable.liquidLost(liquid.getFluid(), liquid.getAmount());
            }
        }

        @Override
        public int getDestination() {
            return info.destinationint;
        }

        @Override
        public void setDestination(int destination) {
            info.destinationint = destination;
            Level level = container != null ? container.getLevel() : null;
            if (level != null && level.isClientSide()) {
                info.destinationUUID = null;
                return;
            }
            IRouter router = RouterManager.get().getServerRouter(destination);
            info.destinationUUID = router != null ? router.getId() : null;
        }

        @Override
        public void setDoNotBuffer(boolean isBuffered) {
            info.doNotBuffer = isBuffered;
        }

        @Override
        public boolean getDoNotBuffer() {
            return info.doNotBuffer;
        }

        @Override
        public void setArrived(boolean flag) {
            info.arrived = flag;
        }

        @Override
        public boolean getArrived() {
            return info.arrived;
        }

        /**
         * Keeps {@code itemsToTake} of the stack in this item and sends the rest back into the pipe
         * as a new item with no destination.
         */
        @Override
        public void split(int itemsToTake, Direction orientation) {
            if (getItemIdentifierStack().getItem().isFluidContainer()) {
                throw new UnsupportedOperationException("Can't split up a FluidContainer");
            }
            ItemIdentifierStack stackToKeep = getItemIdentifierStack();
            ItemIdentifierStack stackToSend = new ItemIdentifierStack(stackToKeep);
            stackToKeep.setStackSize(itemsToTake);
            stackToSend.setStackSize(stackToSend.getStackSize() - itemsToTake);

            newId();

            LPTravelingItemServer newItem = new LPTravelingItemServer(stackToSend);
            newItem.setSpeed(getSpeed());
            newItem.setTransportMode(getTransportMode());
            newItem.setDestination(getDestination());
            newItem.clearDestination();

            if (container instanceof LogisticsGenericPipeBlockEntity pipe && pipe.pipe != null) {
                pipe.pipe.transport.injectItem((LPTravelingItem) newItem, orientation);
            }
        }

        @Override
        public void setTransportMode(TransportMode transportMode) {
            info.transportMode = transportMode;
        }

        @Override
        public TransportMode getTransportMode() {
            return info.transportMode;
        }

        @Override
        public void addToJamList(IRouter router) {
            info.jamlist.add(router.getSimpleID());
        }

        @Override
        public List<Integer> getJamList() {
            return info.jamlist;
        }

        @Override
        public int getBufferCounter() {
            return info.bufferCounter;
        }

        @Override
        public void setBufferCounter(int counter) {
            info.bufferCounter = counter;
        }

        @Override
        public @Nullable UUID getDestinationUUID() {
            return info.destinationUUID;
        }

        /**
         * Finds the destination's router again by its UUID if the number it was known by now
         * belongs to another router, or to none.
         */
        @Override
        public void checkIDFromUUID() {
            IRouter router = RouterManager.get().getRouter(info.destinationint);
            if (router == null || !router.getId().equals(info.destinationUUID)) {
                info.destinationint = RouterManager.get().getIDforUUID(info.destinationUUID);
            }
        }

        @Override
        public void setDistanceTracker(IDistanceTracker tracker) {
            info.tracker = tracker;
        }

        @Override
        public @Nullable IDistanceTracker getDistanceTracker() {
            return info.tracker;
        }

        public void resetDelay() {
            info.resetDelay();
        }

        @Override
        public void setAdditionalTargetInformation(@Nullable IAdditionalTargetInformation targetInfo) {
            info.targetInfo = targetInfo;
        }

        @Override
        public @Nullable IAdditionalTargetInformation getAdditionalTargetInformation() {
            return info.targetInfo;
        }

        @Override
        public LPTravelingItem renderCopy() {
            throw new UnsupportedOperationException();
        }
    }
}
