package logisticspipes.routing.pathfinder;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface IPipeInformationProvider {

	boolean isCorrect(ConnectionType type);

	int getX();

	int getY();

	int getZ();

	@Nullable Level getWorld();

	boolean isRouterInitialized();

	boolean isRoutingPipe();

	CoreRoutedPipe getRoutingPipe();

	BlockEntity getNextConnectedTile(Direction direction);

	boolean isFirewallPipe();

	IFilter getFirewallFilter();

	BlockEntity getTile();

	boolean divideNetwork();

	boolean powerOnly();

	boolean isOnewayPipe();

	boolean isOutputClosed(Direction direction);

	boolean canConnect(BlockEntity to, Direction direction, boolean flag);

	double getDistance();

	double getDistanceWeight();

	boolean isItemPipe();

	boolean isFluidPipe();

	boolean isPowerPipe();

	double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double travled, double max, List<DoubleCoordinates> visited);

	boolean acceptItem(LPTravelingItem item, BlockEntity from);

	void refreshTileCacheOnSide(Direction side);

	boolean isMultiBlock();

	Stream<BlockEntity> getPartsOfPipe();
}
