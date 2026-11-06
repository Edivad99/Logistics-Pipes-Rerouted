package logisticspipes.transport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.LPEventListener;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.connection.special.SpecialBlockEntityConnection;
import logisticspipes.interfaces.IBufferItems;
import logisticspipes.interfaces.IItemAdvancedExistence;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.ITargetSlotInformation;
import logisticspipes.inventory.IInventoryUtil;
import logisticspipes.inventory.InventoryUtilFactory;
import logisticspipes.inventory.transactor.ITransactor;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PipeItemBufferMessage;
import logisticspipes.network.to_client.pipe.TravellingItemContentMessage;
import logisticspipes.network.to_client.pipe.TravellingItemPositionMessage;
import logisticspipes.network.to_server.pipe.RequestPipeContentMessage;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.routing.item.ItemRoutingInformation;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.util.ItemStackLoader;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.SyncList;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

/**
 * Moves the items through one pipe: each enters on a side, travels along the pipe and, at the end,
 * goes on into the block on its exit side.
 *
 * <p>On the server, a routed pipe picks the exit from the item's destination and an unrouted one
 * picks any pipe around at random; an item with nowhere to go waits in the buffer and tries again,
 * or is dropped. The clients watching the pipe are told about every item that enters, and move
 * their own copies in the meantime.</p>
 */
public class PipeTransportLogistics {

    /**
     * Where an item leaves the pipe: {@code face}, or no side when it has none. Without
     * {@code hasRoute} the pipe has taken care of the item already, buffering or dropping it, and
     * the caller leaves it alone.
     */
    record RoutingResult(@Nullable Direction face, boolean hasRoute) {
    }

    private static final String TRAVELING_ITEMS_KEY = "travelingEntities";
    private static final String BUFFER_KEY = "buffercontents";

    /**
     * Ticks a buffered item waits before trying again.
     */
    private static final int BUFFER_TIMEOUT = SharedConstants.TICKS_PER_SECOND * 2;

    /**
     * Times an item whose destination cannot be reached is buffered before it is sent on anyway.
     */
    private static final int MAX_DESTINATION_UNREACHABLE_BUFFER = 30;

    /**
     * What is waiting to be retried. The client's copy, which {@link #setClientItemBuffer} fills for
     * display, only has the stacks: the tick loop that reads the rest is server-only.
     */
    public final SyncList<BufferedItem> itemBuffer = new SyncList<>();
    public final LPItemList items = new LPItemList(this);
    public final boolean isRouted;
    @Setter
    protected LogisticsGenericPipeBlockEntity container;
    private @Nullable LevelChunk chunk;

    public PipeTransportLogistics(boolean isRouted) {
        this.isRouted = isRouted;
    }

    public void initialize() {
        Level level = getWorld();
        if (level == null || level.isClientSide()) {
            return;
        }
        // Cache the chunk for marking dirty, without forcing a load: initialize() runs while the
        // chunk is still being loaded, and getChunkAt() would re-enter the chunk system asking for
        // FULL status. getChunkNow returns null when it is not ready, which markChunkModified
        // already tolerates.
        chunk = chunkNow(level, container.getBlockPos());
        itemBuffer.syncTo(level, container.getBlockPos(), buffered -> new PipeItemBufferMessage(
            container.getBlockPos(),
            buffered.stream().map(BufferedItem::stack).toList()));
    }

    /**
     * Marks the chunks of this pipe and of {@code blockEntity} as changed when an item crosses from
     * one into the other, so both are saved with the item where it is now.
     */
    public void markChunkModified(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null || chunk == null) {
            return;
        }
        BlockPos here = container.getBlockPos();
        BlockPos there = blockEntity.getBlockPos();
        if (SectionPos.blockToSectionCoord(here.getX()) == SectionPos.blockToSectionCoord(there.getX())
            && SectionPos.blockToSectionCoord(here.getZ()) == SectionPos.blockToSectionCoord(there.getZ())) {
            return;
        }
        chunk.markUnsaved();
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipe && pipe.pipe != null
            && pipe.pipe.transport.chunk != null) {
            pipe.pipe.transport.chunk.markUnsaved();
            return;
        }
        // Same reasoning as in initialize(): never force-load the neighbor's chunk just to flag it
        // dirty. If it is not loaded there is nothing to flag.
        LevelChunk neighbourChunk = chunkNow(level(), there);
        if (neighbourChunk != null) {
            neighbourChunk.markUnsaved();
        }
    }

    private static @Nullable LevelChunk chunkNow(Level level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getZ()));
    }

    protected @Nullable CoreUnroutedPipe getPipe() {
        return container.pipe;
    }

    protected CoreRoutedPipe getRoutedPipe() {
        if (!isRouted) {
            throw new UnsupportedOperationException("Can't use a Transport pipe as a routing pipe");
        }
        return (CoreRoutedPipe) pipe();
    }

    /**
     * The pipe, for the code that only runs once it is built.
     */
    private CoreUnroutedPipe pipe() {
        return Objects.requireNonNull(container.pipe, "Moving items in a pipe that is not built yet");
    }

    @Nullable
    public Level getWorld() {
        return container.getLevel();
    }

    /**
     * The level, for the code that only runs in one.
     */
    private Level level() {
        return Objects.requireNonNull(getWorld(), "Moving items in a pipe outside a level");
    }

    /**
     * Replaces what the client thinks is buffered. The timeout and travelling item stay server-side.
     */
    public void setClientItemBuffer(List<ItemIdentifierStack> contents) {
        itemBuffer.clear();
        contents.forEach(stack -> itemBuffer.add(new BufferedItem(stack, 0, 0, null)));
    }

    public void updateEntity() {
        moveSolids();
        if (!level().isClientSide()) {
            retryBufferedItems();
            itemBuffer.sendUpdateToWatchers();
        }
    }

    /**
     * Sends on the buffered items whose wait is over: the ones that can now reach their destination,
     * or have waited for it too often, and the bare stacks, which go out with no destination.
     */
    private void retryBufferedItems() {
        if (itemBuffer.isEmpty()) {
            return;
        }
        List<LPTravelingItem> toSend = new ArrayList<>();
        Iterator<BufferedItem> iterator = itemBuffer.iterator();
        while (iterator.hasNext()) {
            BufferedItem next = iterator.next();
            LPTravelingItemServer traveling = next.item;
            if (next.timeOut > 0) {
                next.timeOut--;
            } else if (traveling == null) {
                LPTravelingItemServer item = new LPTravelingItemServer(next.stack);
                item.setDoNotBuffer(true);
                item.setBufferCounter(next.bufferCounter + 1);
                toSend.add(item);
                iterator.remove();
            } else if (next.bufferCounter > MAX_DESTINATION_UNREACHABLE_BUFFER
                || getRoutedPipe().getRouter().hasRoute(traveling.getDestination(),
                traveling.getTransportMode() == TransportMode.Active, traveling.getItemIdentifierStack().getItem())) {
                traveling.setBufferCounter(next.bufferCounter + 1);
                toSend.add(traveling);
                iterator.remove();
            } else {
                next.bufferCounter++;
                next.timeOut = BUFFER_TIMEOUT;
            }
        }
        for (LPTravelingItem item : toSend) {
            injectItem(item, Direction.UP);
        }
    }

    /**
     * Drops everything waiting in the buffer on the pipe's block.
     */
    public void dropBuffer() {
        Level level = level();
        BlockPos pos = container.getBlockPos();
        Iterator<BufferedItem> iterator = itemBuffer.iterator();
        while (iterator.hasNext()) {
            ItemIdentifierStack stack = iterator.next().stack;
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                stack.makeNormalStack()));
            iterator.remove();
        }
    }

    public float getPipeLength() {
        return 1.0F;
    }

    public double getDistanceWeight() {
        return 1.0D;
    }

    public float getYawDiff(LPTravelingItem item) {
        return 0.0F;
    }

    /**
     * The side an item traveling towards {@code input} leaves by, when the pipe has only one way to
     * send it and the client can tell without asking the server.
     *
     * @return the exit, or null when the pipe routes items and only the server knows
     */
    public @Nullable Direction getFixedExit(Direction input) {
        return null;
    }

    /**
     * The speed every item moves at in this pipe, so the client can match the server without
     * waiting for it.
     *
     * @return the speed, or 0 when it depends on the item and only the server knows
     */
    protected float getFixedSpeed() {
        return 0;
    }

    public void sendItem(ItemStack stackToSend) {
        injectItem(new LPTravelingItemServer(stackToSend), Direction.UP);
    }

    public int injectItem(LPTravelingItemServer item, @Nullable Direction inputOrientation) {
        return injectItem((LPTravelingItem) item, inputOrientation);
    }

    public int injectItem(IRoutedItem item, @Nullable Direction inputOrientation) {
        return injectItem((LPTravelingItem) item, inputOrientation);
    }

    /**
     * Puts the item into the pipe, coming in towards {@code inputOrientation}. On the server this
     * picks where it goes, which may instead buffer or drop it.
     *
     * @return how many of the item were taken
     */
    public int injectItem(LPTravelingItem item, @Nullable Direction inputOrientation) {
        if (item.isCorrupted()) {
            // Nothing left in it, or a client copy not described yet: adding it would only fail later.
            return 0;
        }
        pipe().triggerDebug();

        int originalCount = stackSize(item);
        item.input = inputOrientation;

        if (!level().isClientSide()) {
            LPTravelingItemServer serverItem = (LPTravelingItemServer) item;
            readjustSpeed(serverItem);
            ItemRoutingInformation infoBefore = serverItem.getInfo().clone();
            RoutingResult result = resolveDestination(serverItem);
            item.output = result.face();
            if (!result.hasRoute()) {
                return 0;
            }
            pipe().debug.log("Injected Item: [" + item.input + ", " + item.output + "] (" + infoBefore);
        } else {
            // The server picks the exit and the speed, and sends them in the next position packet. A
            // pipe that can only send the item one way already knows both, which keeps the item
            // drawn and moving smoothly until then.
            item.output = inputOrientation == null ? null : getFixedExit(inputOrientation);
            if (getFixedSpeed() > 0) {
                item.setSpeed(getFixedSpeed());
            }
        }

        if (item.getPosition() >= getPipeLength()) {
            // The leftover position from the previous pipe can already be past this one's end.
            // Without an exit, which a client copy usually is, handle it the way moveSolids does
            // instead of asking for the block on a null side.
            if (item.output != null) {
                reachedEnd(item);
            } else if (!level().isClientSide()) {
                dropItem((LPTravelingItemServer) item);
            }
        } else {
            items.add(item);
            if (!level().isClientSide() && !pipe().isOpaque() && stackSize(item) > 0) {
                sendItemPacket((LPTravelingItemServer) item);
            }
        }
        return originalCount - stackSize(item);
    }

    /**
     * How many there are in the item, 0 for a client copy the server has not described yet.
     */
    private static int stackSize(LPTravelingItem item) {
        ItemIdentifierStack stack = item.getItemIdentifierStack();
        return stack != null ? stack.getStackSize() : 0;
    }

    /**
     * Sends an item that bounced off an inventory back the way it came, under a new id and with a
     * new route. A pipe with a buffer takes what it can first.
     */
    protected void reverseItem(LPTravelingItemServer item) {
        if (item.isCorrupted()) {
            return;
        }
        if (getPipe() instanceof IBufferItems buffer) {
            item.getItemIdentifierStack().setStackSize(
                buffer.addToBuffer(item.getItemIdentifierStack(), item.getAdditionalTargetInformation()));
            if (item.getItemIdentifierStack().getStackSize() <= 0) {
                return;
            }
        }

        // A new id, so the clients are told the stack again: it may have shrunk.
        item.newId();
        if (item.getPosition() >= getPipeLength()) {
            item.setPosition(item.getPosition() - getPipeLength());
        }
        item.input = Objects.requireNonNull(item.output, "Reversing an item with no exit").getOpposite();

        readjustSpeed(item);
        RoutingResult result = resolveDestination(item);
        item.output = result.face();
        if (!result.hasRoute()) {
            return;
        }
        if (item.output == null) {
            dropItem(item);
            return;
        }

        items.unscheduleRemoval(item);
        if (!pipe().isOpaque()) {
            sendItemPacket(item);
        }
    }

    RoutingResult resolveDestination(LPTravelingItemServer data) {
        return isRouted ? resolveRoutedDestination(data) : resolveUnroutedDestination(data);
    }

    /**
     * Any pipe around that takes the item, other than the one it came from.
     */
    RoutingResult resolveUnroutedDestination(LPTravelingItemServer data) {
        Level level = level();
        Direction cameFrom = data.input != null ? data.input.getOpposite() : null;
        List<Direction> exits = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir == cameFrom) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(container.getBlockPos().relative(dir));
            if (!PipeInformationManager.isPipe(blockEntity) || !canPipeConnect(blockEntity, dir)) {
                continue;
            }
            if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipe
                && !pipe.canConnect(container, dir.getOpposite(), false)) {
                continue;
            }
            exits.add(dir);
        }
        if (exits.isEmpty()) {
            return new RoutingResult(null, false);
        }
        return new RoutingResult(exits.get(level.getRandom().nextInt(exits.size())), true);
    }

    /**
     * The side towards the item's destination. An item with no destination gets one if the pipe is
     * waiting for it; one whose destination cannot be reached right now, or that has no way to go,
     * waits in the buffer.
     */
    RoutingResult resolveRoutedDestination(LPTravelingItemServer data) {
        CoreRoutedPipe pipe = getRoutedPipe();
        Direction blocked = null;

        if (data.getDestinationUUID() == null) {
            ItemIdentifierStack stack = data.getItemIdentifierStack();
            ItemRoutingInformation queued = pipe.getQueuedForItemStack(stack);
            if (queued != null) {
                data.setInformation(queued);
                data.getInfo().setItem(stack);
                blocked = data.input != null ? data.input.getOpposite() : null;
            }
        }

        pipe.relayedItem(data.getItemIdentifierStack().getStackSize());

        if (data.getDestination() >= 0 && data.getBufferCounter() < MAX_DESTINATION_UNREACHABLE_BUFFER
            && !pipe.getRouter().hasRoute(data.getDestination(), data.getTransportMode() == TransportMode.Active,
            data.getItemIdentifierStack().getItem())) {
            itemBuffer.add(new BufferedItem(data.getItemIdentifierStack(), BUFFER_TIMEOUT, data.getBufferCounter(),
                data));
            return new RoutingResult(null, false);
        }

        Direction exit;
        if (pipe.stillNeedReplace() || pipe.initialInit()) {
            data.setDoNotBuffer(false);
            exit = null;
        } else {
            exit = pipe.getRouteResolver().getOrientationForItem(data, blocked);
        }

        if (exit == null && !data.getDoNotBuffer() && data.getBufferCounter() < 5) {
            // An item that already knows where it goes, say one arriving at a pipe whose chunk has
            // just loaded, keeps that: buffered as a bare stack it would come back with no destination
            LPTravelingItemServer keep = data.getDestinationUUID() != null ? data : null;
            itemBuffer.add(new BufferedItem(data.getItemIdentifierStack(), BUFFER_TIMEOUT, data.getBufferCounter(),
                keep));
            return new RoutingResult(null, false);
        }

        if (exit != null && !pipe.getRouter().isRoutedExit(exit) && isItemUnwanted(data.getItemIdentifierStack())) {
            return new RoutingResult(null, false);
        }

        data.resetDelay();
        return new RoutingResult(exit, true);
    }

    public void readjustSpeed(LPTravelingItemServer item) {
        if (!isRouted) {
            return;
        }
        float boost = switch (item.getTransportMode()) {
            case Default, Unknown -> 20F;
            case Passive -> 25F;
            case Active -> 30F;
        };
        int speedUpgrades = getRoutedPipe().getUpgradeManager().getSpeedUpgradeCount();
        float speedMultiplier = 1.0F + 0.02F * speedUpgrades;
        float powerMultiplier = 1.0F + 0.03F * speedUpgrades;

        float add = Math.max(item.getSpeed(), LPConstants.PIPE_NORMAL_SPEED * boost * powerMultiplier)
            - item.getSpeed();
        if (getRoutedPipe().useEnergy((int) (add * 50 + 0.5))) {
            item.setSpeed(Math.min(Math.max(item.getSpeed(), LPConstants.PIPE_NORMAL_SPEED * boost * speedMultiplier),
                1.0F));
        }
    }

    /**
     * Drops what {@link #serialize} writes for the server alone from a copy of the saved data meant
     * for a client. The traveling items there are server items, and the client learns about items
     * and the buffer through their own packets.
     */
    public static void removeServerOnlyData(CompoundTag tag) {
        tag.remove(TRAVELING_ITEMS_KEY);
        tag.remove(BUFFER_KEY);
    }

    public void deserialize(ValueInput input) {
        for (ValueInput dataTag : input.childrenListOrEmpty(TRAVELING_ITEMS_KEY)) {
            try {
                LPTravelingItem item = new LPTravelingItemServer(dataTag);
                if (item.isCorrupted()) {
                    LogisticsPipes.LOG.warn("Dropped a traveling item whose stack could not be read");
                    continue;
                }
                items.scheduleLoad(item);
            } catch (Throwable t) {
                // It may be the case that entities cannot be reloaded between two versions.
                LogisticsPipes.LOG.warn("Dropped a traveling item that could not be loaded", t);
            }
        }

        itemBuffer.clear();
        for (ValueInput bufferEntry : input.childrenListOrEmpty(BUFFER_KEY)) {
            ItemStack stack = ItemStackLoader.loadItemStack(bufferEntry);
            itemBuffer.add(new BufferedItem(ItemIdentifierStack.getFromStack(stack), BUFFER_TIMEOUT, 0, null));
        }
    }

    public void serialize(ValueOutput output) {
        ValueOutput.ValueOutputList travelingEntities = output.childrenList(TRAVELING_ITEMS_KEY);
        for (LPTravelingItem item : items.itemsToSave()) {
            if (item instanceof LPTravelingItemServer serverItem) {
                serverItem.serialize(travelingEntities.addChild());
            }
        }

        ValueOutput.ValueOutputList bufferContents = output.childrenList(BUFFER_KEY);
        for (BufferedItem buffered : itemBuffer) {
            ItemStackLoader.saveItemStack(bufferContents.addChild(), buffered.stack.makeNormalStack());
        }
    }

    /**
     * The block an item leaving the pipe through {@code output} goes into.
     */
    protected @Nullable BlockEntity getBlockEntityAtExit(Direction output) {
        return container.getBlockEntity(output);
    }

    @Nullable
    public CoreUnroutedPipe getNextPipe(Direction output) {
        if (getBlockEntityAtExit(output) instanceof LogisticsGenericPipeBlockEntity pipe) {
            return pipe.pipe;
        }
        return null;
    }

    /**
     * Takes the item out of the pipe, at its end, and hands it to the block on its exit side.
     */
    protected void reachedEnd(LPTravelingItem item) {
        Direction exit = Objects.requireNonNull(item.output, "An item reached the end of a pipe with no exit");
        BlockEntity blockEntity = getBlockEntityAtExit(exit);
        if (!items.scheduleRemoval(item)) {
            return;
        }
        if (!level().isClientSide()) {
            handleBlockEntityReachedServer((LPTravelingItemServer) item, blockEntity, exit);
        } else {
            handleBlockEntityReachedClient((LPTravelingItemClient) item, blockEntity, exit);
        }
    }

    protected void handleBlockEntityReachedServer(LPTravelingItemServer arrivingItem,
        @Nullable BlockEntity blockEntity, Direction dir) {
        handleBlockEntityReachedServer_internal(arrivingItem, blockEntity, dir);
    }

    /**
     * Hands the item to the block on its exit side: the next pipe, or an inventory, where it goes in
     * through the side the upgrades pick and what does not fit comes back. Anything else, or no
     * block at all, drops it.
     *
     * <p>Final and apart from {@link #handleBlockEntityReachedServer}, so a transport that changes
     * that can still fall back on this.</p>
     */
    protected final void handleBlockEntityReachedServer_internal(LPTravelingItemServer arrivingItem,
        @Nullable BlockEntity blockEntity, Direction dir) {
        if (getPipe() instanceof PipeItemsFluidSupplier fluidSupplier) {
            fluidSupplier.endReached(arrivingItem, blockEntity);
            if (arrivingItem.getItemIdentifierStack().getStackSize() <= 0) {
                return;
            }
        }

        markChunkModified(blockEntity);
        if (isRouted && arrivingItem.getArrived()) {
            getRoutedPipe().notifyOfItemArrival(arrivingItem.getInfo());
        }
        if (getPipe() instanceof FluidRoutedPipe fluidPipe && fluidPipe.endReached(arrivingItem, blockEntity)) {
            return;
        }
        boolean isSpecialConnectionInformationTransition = false;
        if (SpecialBlockEntityConnection.get().needsInformationTransition(blockEntity)) {
            isSpecialConnectionInformationTransition = true;
            SpecialBlockEntityConnection.get().transmit(Objects.requireNonNull(blockEntity), arrivingItem);
        }
        if (PipeInformationManager.isPipe(blockEntity)) {
            if (passToNextPipe(arrivingItem, blockEntity)) {
                return;
            }
            dropItem(arrivingItem);
            return;
        }

        IInventoryUtil util = InventoryUtilFactory.get().getInventoryUtil(blockEntity, dir.getOpposite());
        if (util == null || !isRouted) {
            dropItem(arrivingItem);
            return;
        }
        insertIntoInventory(arrivingItem, Objects.requireNonNull(blockEntity), util,
            isSpecialConnectionInformationTransition);
    }

    private void insertIntoInventory(LPTravelingItemServer arrivingItem, BlockEntity blockEntity,
        IInventoryUtil util, boolean isSpecialConnectionInformationTransition) {
        CoreRoutedPipe pipe = getRoutedPipe();
        pipe.getCacheHolder().trigger(CacheTypes.Inventory);

        // An item that may not exist outside the pipes is destroyed instead.
        if (!isSpecialConnectionInformationTransition && isItemUnwanted(arrivingItem.getItemIdentifierStack())) {
            return;
        }
        // Last chance for a chassis to back out.
        if (arrivingItem.getTransportMode() != TransportMode.Active
            && !pipe.getDeliveryHandler().stillWantItem(arrivingItem)) {
            reverseItem(arrivingItem);
            return;
        }

        ISlotUpgradeManager slotManager;
        if (arrivingItem.getInfo().targetInfo instanceof ChassiTargetInformation chassiTarget) {
            slotManager = pipe.getUpgradeManager(ModulePositionType.SLOT, chassiTarget.getModuleSlot());
        } else {
            if (pipe instanceof PipeLogisticsChassis) {
                // An item bounced back into the chassis's own inventory, or one whose route was lost,
                // no longer belongs to a module; it goes in under the pipe's upgrades.
                LogisticsPipes.LOG.debug("[ItemInsertion] info not for chassis pipe: {}", arrivingItem);
            }
            slotManager = pipe.getOriginalUpgradeManager();
        }

        if (arrivingItem.getAdditionalTargetInformation() instanceof ITargetSlotInformation information
            && util instanceof ISpecialInsertion specialInsertion) {
            int slot = information.getTargetSlot();
            if (util.getContainerSize() > slot) {
                ItemStack toAdd = arrivingItem.getItemIdentifierStack().makeNormalStack();
                int amountLeft = Math.max(0, information.getAmount() - util.getItem(slot).getCount());
                toAdd.setCount(Math.min(toAdd.getCount(), amountLeft));
                if (toAdd.getCount() > 0) {
                    arrivingItem.getItemIdentifierStack().lowerStackSize(specialInsertion.addToSlot(toAdd, slot));
                }
            }
            if (information.isLimited()) {
                if (arrivingItem.getItemIdentifierStack().getStackSize() > 0) {
                    reverseItem(arrivingItem);
                }
                return;
            }
        }

        if (!pipe.getUpgradeManager().hasCombinedSneakyUpgrade() || slotManager.hasOwnSneakyUpgrade()) {
            Direction insertion = Objects.requireNonNull(arrivingItem.output).getOpposite();
            if (slotManager.hasSneakyUpgrade()) {
                insertion = slotManager.getSneakyOrientation();
            }
            if (insertArrivingItem(arrivingItem, blockEntity, insertion)) {
                return;
            }
        } else {
            @Nullable Direction @Nullable [] sides = pipe.getUpgradeManager().getCombinedSneakyOrientation();
            if (sides != null) {
                for (Direction insertion : sides) {
                    if (insertion != null && insertArrivingItem(arrivingItem, blockEntity, insertion)) {
                        return;
                    }
                }
            }
        }

        if (arrivingItem.getItemIdentifierStack().getStackSize() > 0) {
            reverseItem(arrivingItem);
        }
    }

    /**
     * @return true, if every item has been inserted and otherwise false.
     */
    private boolean insertArrivingItem(LPTravelingItemServer arrivingItem, BlockEntity blockEntity,
        @Nullable Direction insertion) {
        // A sneaky upgrade points at a side of its own choosing, which need not be the side the item
        // arrived from -- and a block can expose an item handler on one face and not another. No
        // handler there means nothing was inserted, so the caller moves on to the next side.
        ITransactor transactor = InventoryUtilFactory.get().getTransactor(blockEntity, insertion);
        if (transactor == null) {
            return false;
        }
        ItemStack added = transactor.add(arrivingItem.getItemIdentifierStack().makeNormalStack(), insertion, true);

        if (!added.isEmpty()) {
            arrivingItem.getItemIdentifierStack().lowerStackSize(added.getCount());
            arrivingItem.setBufferCounter(0);
        }

        ItemRoutingInformation info = arrivingItem.getInfo();
        boolean isSplitStack = arrivingItem.getItemIdentifierStack().getStackSize() > 0;
        if (isSplitStack) {
            // Some is left over, so the stack splits and the part that went in needs its own info.
            info = info.clone();
        }
        info.getItem().setStackSize(added.getCount());

        inventorySystemConnectorHook(info, blockEntity);
        return !isSplitStack;
    }

    protected void handleBlockEntityReachedClient(LPTravelingItemClient arrivingItem,
        @Nullable BlockEntity blockEntity, Direction dir) {
        // The client only follows an item into the next pipe; the server says what happens otherwise.
        if (PipeInformationManager.isPipe(blockEntity)) {
            passToNextPipe(arrivingItem, blockEntity);
        }
    }

    /**
     * Whether the item may not leave the pipes into a normal inventory.
     */
    protected boolean isItemUnwanted(ItemIdentifierStack stack) {
        ItemStack itemStack = stack.makeNormalStack();
        return itemStack.getItem() instanceof IItemAdvancedExistence existence
            && !existence.canExistInNormalInventory(itemStack);
    }

    protected void inventorySystemConnectorHook(ItemRoutingInformation info, BlockEntity blockEntity) {
    }

    public boolean canPipeConnect(BlockEntity blockEntity, Direction side) {
        return canPipeConnect_internal(blockEntity, side);
    }

    /**
     * Connects to other pipes, except high speed tubes; a routed pipe also connects to inventories,
     * to LP blocks and, from the side, to power providers.
     *
     * <p>Final and apart from {@link #canPipeConnect}, so a transport that changes that can still
     * fall back on this.</p>
     */
    public final boolean canPipeConnect_internal(BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipe && pipe.pipe != null && pipe.pipe.isHSTube()
            && !pipe.pipe.actAsNormalPipe()) {
            return false;
        }
        if (!isRouted) {
            return PipeInformationManager.isPipe(blockEntity);
        }
        if (blockEntity instanceof ILogisticsPowerProvider || blockEntity instanceof ISubSystemPowerProvider) {
            Direction ori = OrientationsUtil.getOrientation(container, blockEntity);
            if (ori != null) {
                boolean sideOnly = blockEntity instanceof LogisticsPowerJunctionBlockEntity
                    || blockEntity instanceof ISubSystemPowerProvider;
                return !(sideOnly && ori.getAxis() == Direction.Axis.Y);
            }
        }
        return InventoryUtilFactory.get().getInventoryUtil(blockEntity, side.getOpposite()) != null
            || blockEntity instanceof LogisticsSolidBlockEntity
            || PipeInformationManager.isPipe(blockEntity);
    }

    private void moveSolids() {
        Level level = level();
        int tick = LPTickHandler.getTick(level);
        items.forEachDeferred(item -> {
            if (item.lastTicked >= tick) {
                return;
            }
            item.lastTicked = tick;
            item.setPosition(item.getPosition() + item.getSpeed());
            if (!hasReachedEnd(item)) {
                return;
            }
            if (item.output == null) {
                if (!level.isClientSide()) {
                    dropItem((LPTravelingItemServer) item);
                }
                items.scheduleRemoval(item);
            } else {
                reachedEnd(item);
            }
        });
    }

    /**
     * Whether the item is done with this pipe: at its end, or, on the server, past the middle when
     * it has nowhere to go and is about to be dropped.
     *
     * <p>A client copy never knows its exit until the server says so: it has none from the moment
     * it enters the pipe until the next position packet. Taking it out at the middle in the
     * meantime made it vanish until that packet came, which an item going as fast as in a high
     * speed tube does within a tick.</p>
     */
    protected boolean hasReachedEnd(LPTravelingItem item) {
        boolean dropping = item.output == null && !level().isClientSide();
        return item.getPosition() >= (dropping ? 0.75F : getPipeLength());
    }

    /**
     * Moves the item on into the pipe at {@code blockEntity}, carrying over how far past this pipe's
     * end it already is.
     *
     * @return whether the next pipe took it: false when there is no pipe there
     */
    protected boolean passToNextPipe(LPTravelingItem item, @Nullable BlockEntity blockEntity) {
        IPipeInformationProvider information = PipeInformationManager.getInformationProviderFor(blockEntity);
        if (information == null) {
            return false;
        }
        item.setPosition(item.getPosition() - getPipeLength());
        item.setYaw(item.getYaw() + getYawDiff(item));
        return information.acceptItem(item, container);
    }

    private void dropItem(LPTravelingItemServer item) {
        if (level().isClientSide()) {
            return;
        }
        item.setSpeed(0.05F);
        item.setContainer(container);
        ItemEntity entity = item.toEntityItem();
        if (entity != null) {
            level().addFreshEntity(entity);
        }
    }

    public NonNullList<ItemStack> dropContents() {
        NonNullList<ItemStack> list = NonNullList.create();
        if (!level().isClientSide()) {
            for (LPTravelingItem item : items) {
                if (item instanceof LPTravelingItemServer serverItem) {
                    list.add(serverItem.getItemIdentifierStack().makeNormalStack());
                }
            }
        }
        return list;
    }

    public boolean delveIntoUnloadedChunks() {
        return true;
    }

    /**
     * Tells the clients watching the pipe where the item is, and what it is the first time they
     * hear of its id.
     */
    private void sendItemPacket(LPTravelingItemServer item) {
        if (!LPEventListener.isAnyoneWatching(container.getBlockPos())) {
            return;
        }
        if (!LPTravelingItem.clientSideKnownIDs.get(item.getId())) {
            TargetLookup.sendToChunkWatchers(container,
                new TravellingItemContentMessage(item.getId(), item.getItemIdentifierStack()));
            LPTravelingItem.clientSideKnownIDs.set(item.getId());
        }
        TargetLookup.sendToChunkWatchers(container, new TravellingItemPositionMessage(
            container.getBlockPos(),
            item.getId(),
            new TravellingItemPositionMessage.Motion(item.getPosition(), item.getSpeed(), item.getYaw()),
            Optional.ofNullable(item.input),
            Optional.ofNullable(item.output)));
    }

    /**
     * Puts the client copy of an item in this pipe, as the server says it is, creating it if the
     * client has not heard of the id yet and asking the server what it is.
     */
    public void handleItemPositionPacket(int travelId, @Nullable Direction input, @Nullable Direction output,
        float speed, float position, float yaw) {
        LPTravelingItemClient item = LPTravelingItem.clientList.get(travelId);
        if (item == null) {
            ClientPacketDistributor.sendToServer(new RequestPipeContentMessage(travelId));
            item = new LPTravelingItemClient(travelId, position, input, output, yaw);
            item.setSpeed(speed);
            LPTravelingItem.clientList.put(travelId, item);
            // A new item has not moved yet, so it does not move again this tick.
            item.lastTicked = LPTickHandler.getClientTick();
        } else if (item.getContainer() instanceof LogisticsGenericPipeBlockEntity oldPipe && oldPipe.pipe != null) {
            oldPipe.pipe.transport.items.remove(item);
            // The packet arrives about a tick after the server moved the item, and the client copy
            // has moved on meanwhile: taking the server's position would set it back and pause it at
            // every pipe, so it keeps its own unless the server's is further.
            float adjustedPosition;
            if (!oldPipe.getBlockPos().equals(container.getBlockPos())) {
                // Still in the previous pipe here: carry on from how far past its end it is.
                adjustedPosition = Math.max(0.0F, item.getPosition() - oldPipe.pipe.transport.getPipeLength());
            } else {
                adjustedPosition = Math.max(position, item.getPosition());
            }
            item.updateInformation(input, output, speed, adjustedPosition, yaw);
            // An item already on its way keeps its last tick: resetting it would pause it here.
        } else {
            item.updateInformation(input, output, speed, position, yaw);
        }
        if (items.get(travelId) == null) {
            items.add(item);
        }
    }

    public void onNeighborBlockChange() {
    }

    public void onBlockPlaced() {
    }

    /**
     * An item waiting in the pipe to be sent on again.
     */
    public static final class BufferedItem {

        private final ItemIdentifierStack stack;
        /**
         * Ticks left before the next try.
         */
        private int timeOut;
        /**
         * How many times the item has been buffered so far.
         */
        private int bufferCounter;
        /**
         * The traveling item to send on, or null to make a fresh one from the stack.
         */
        private final @Nullable LPTravelingItemServer item;

        private BufferedItem(ItemIdentifierStack stack, int timeOut, int bufferCounter,
            @Nullable LPTravelingItemServer item) {
            this.stack = stack;
            this.timeOut = timeOut;
            this.bufferCounter = bufferCounter;
            this.item = item;
        }

        public ItemIdentifierStack stack() {
            return stack;
        }
    }
}
