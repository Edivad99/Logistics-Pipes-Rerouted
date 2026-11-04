package logisticspipes.routing.pathfinder;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.ConnectionType;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.item.ItemIdentifier;

public interface IPipeInformationProvider {

    boolean isCorrect(ConnectionType type);

    BlockPos getPos();

    @Nullable Level getLevel();

    boolean isRouterInitialized();

    boolean isRoutingPipe();

    CoreRoutedPipe getRoutingPipe();

    @Nullable
    BlockEntity getNextConnectedBlockEntity(Direction direction);

    boolean isFirewallPipe();

    IFilter getFirewallFilter();

    BlockEntity getBlockEntity();

    boolean divideNetwork();

    boolean powerOnly();

    boolean isOnewayPipe();

    boolean isOutputClosed(Direction direction);

    /**
     * Whether this pipe would connect to {@code to} through {@code direction}.
     *
     * @param ignoreSystemDisconnection whether to connect anyway when the router has cut that side
     *     off because the pipe there belongs to another security station
     */
    boolean canConnect(BlockEntity to, Direction direction, boolean ignoreSystemDisconnection);

    double getDistance();

    double getDistanceWeight();

    boolean isItemPipe();

    boolean isFluidPipe();

    boolean isPowerPipe();

    double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double travled,
        double max, List<BlockPos> visited);

    boolean acceptItem(LPTravelingItem item, BlockEntity from);

    void refreshNeighborCache(Direction side);

    boolean isMultiBlock();

    Stream<@Nullable BlockEntity> getPartsOfPipe();
}
