package logisticspipes.integrations.buildcraft;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.content.BuildCraftContentIds;
import buildcraft.api.v2.item.ItemTransferResult;
import buildcraft.api.v2.pipe.ItemInjectionRequest;
import buildcraft.api.v2.pipe.ItemTransitData;
import buildcraft.api.v2.pipe.PipeView;
import buildcraft.transport.pipe.behaviour.PipeBehaviourIron;
import buildcraft.transport.tile.TilePipeHolder;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.ConnectionType;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.item.RoutedItemStacks;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.utils.item.ItemIdentifier;

final class BuildCraftPipeInformation implements IPipeInformationProvider {

    private static final Set<Identifier> DIVIDING = Set.of(
        BuildCraftContentIds.Pipes.OBSIDIAN_ITEM,
        BuildCraftContentIds.Pipes.VOID_ITEM,
        BuildCraftContentIds.Pipes.STRIPES_ITEM);

    private static final Set<Identifier> POWER_ONLY = Set.of(
        BuildCraftContentIds.Pipes.DIAMOND_ITEM,
        BuildCraftContentIds.Pipes.DAIZULI_ITEM);

    /**
     * BuildCraft's public API does not say where an iron pipe points.
     */
    private static final InternalAccess IRON = new InternalAccess("the iron pipe");

    private final BlockEntity blockEntity;
    private final PipeView view;

    BuildCraftPipeInformation(BlockEntity blockEntity, PipeView view) {
        this.blockEntity = blockEntity;
        this.view = view;
    }

    /**
     * Whether BuildCraft carries items in this pipe. Fluid, power and structure pipes stay
     * ordinary blocks to LP.
     */
    static boolean carriesItems(PipeView view) {
        return view.typeId().getPath().endsWith("_item");
    }

    @Override
    public boolean isCorrect(ConnectionType type) {
        return type.isItem();
    }

    @Override
    public BlockPos getPos() {
        return view.position();
    }

    @Override
    public @Nullable Level getLevel() {
        return blockEntity.getLevel();
    }

    @Override
    public boolean isRouterInitialized() {
        return true;
    }

    @Override
    public boolean isRoutingPipe() {
        return false;
    }

    @Override
    public CoreRoutedPipe getRoutingPipe() {
        throw new UnsupportedOperationException("A BuildCraft pipe is not a routing pipe");
    }

    @Override
    public @Nullable BlockEntity getNextConnectedBlockEntity(Direction direction) {
        Level level = getLevel();
        if (level == null || !view.connectedSides().contains(direction)) {
            return null;
        }
        return level.getBlockEntity(getPos().relative(direction));
    }

    @Override
    public boolean isFirewallPipe() {
        return false;
    }

    @Override
    public IFilter getFirewallFilter() {
        throw new UnsupportedOperationException("A BuildCraft pipe is not a firewall pipe");
    }

    @Override
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean divideNetwork() {
        // Without its direction an iron pipe can not be routed through safely
        return DIVIDING.contains(view.typeId()) || (isIron() && IRON.isDisabled());
    }

    @Override
    public boolean powerOnly() {
        return POWER_ONLY.contains(view.typeId());
    }

    @Override
    public boolean isOnewayPipe() {
        return isIron();
    }

    @Override
    public boolean isOutputClosed(Direction direction) {
        if (!isIron()) {
            return false;
        }
        Direction output = IRON.read(() -> {
            if (!(blockEntity instanceof TilePipeHolder holder) || holder.getPipe() == null) {
                return null;
            }
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourIron iron)) {
                return null;
            }
            return iron.getCurrentDir();
        });
        return output != direction;
    }

    private boolean isIron() {
        return view.typeId().equals(BuildCraftContentIds.Pipes.IRON_ITEM);
    }

    @Override
    public boolean canConnect(BlockEntity to, Direction direction, boolean ignoreSystemDisconnection) {
        return view.connectedSides().contains(direction);
    }

    @Override
    public double getDistance() {
        return 1;
    }

    @Override
    public double getDistanceWeight() {
        return 1;
    }

    @Override
    public boolean isItemPipe() {
        return true;
    }

    @Override
    public boolean isFluidPipe() {
        return false;
    }

    @Override
    public boolean isPowerPipe() {
        return false;
    }

    @Override
    public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive,
        double traveled, double max, List<BlockPos> visited) {
        if (traveled >= max) {
            return Integer.MAX_VALUE;
        }
        double lowest = Integer.MAX_VALUE;
        for (Direction direction : view.connectedSides()) {
            if (direction == ignore) {
                continue;
            }
            IPipeInformationProvider next =
                PipeInformationManager.getInformationProviderFor(getNextConnectedBlockEntity(direction));
            if (next == null || visited.contains(next.getPos())) {
                continue;
            }
            visited.add(next.getPos());
            double result = next.getDistanceTo(destinationint, direction.getOpposite(), ident, isActive,
                traveled + getDistance(), Math.min(max, lowest), visited);
            visited.remove(next.getPos());
            if (result != Integer.MAX_VALUE) {
                lowest = Math.min(lowest, result + getDistance());
            }
        }
        return lowest;
    }

    /**
     * Hands the item over to BuildCraft, carrying its routing information on the stack.
     *
     * <p>On the client the item just leaves LP's pipe: BuildCraft draws its own copy.
     *
     * @return false when BuildCraft did not take all of it; the rest is left in {@code item}
     */
    @Override
    public boolean acceptItem(LPTravelingItem item, BlockEntity from) {
        if (!(item instanceof LPTravelingItemServer serverItem)) {
            return true;
        }
        Level level = getLevel();
        if (level == null || item.output == null) {
            return false;
        }
        ItemStack stack = RoutedItemStacks.attach(serverItem.getInfo(), level.registryAccess());
        double speed = item.getSpeed() > 0 ? item.getSpeed() : ItemTransitData.DEFAULT.speedBlocksPerTick();
        ItemInjectionRequest request = new ItemInjectionRequest(stack, item.output.getOpposite(),
            new ItemTransitData(Optional.empty(), speed));
        ItemTransferResult result = BuildCraftPipes.service().injectItem(level, getPos(), request,
            OperationMode.EXECUTE);
        if (result.completed()) {
            return true;
        }
        if (!result.movedAnything()) {
            // Nothing is on its way, so nothing will come back for the stored information
            RoutedItemStacks.detach(stack, level.registryAccess());
            return false;
        }
        // What went in keeps the information; the rest gets its own
        serverItem.setInformation(serverItem.getInfo().clone());
        item.getItemIdentifierStack().setStackSize(result.remainderCount());
        return false;
    }

    @Override
    public void refreshNeighborCache(Direction side) {
    }

    @Override
    public boolean isMultiBlock() {
        return false;
    }

    @Override
    public Stream<@Nullable BlockEntity> getPartsOfPipe() {
        return Stream.empty();
    }
}
