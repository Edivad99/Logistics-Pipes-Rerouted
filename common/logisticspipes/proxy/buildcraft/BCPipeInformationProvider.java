package logisticspipes.proxy.buildcraft;
// TODO: BuildCraft not ported to 1.20.1 — stub

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class BCPipeInformationProvider implements IPipeInformationProvider {
    @Override public boolean isCorrect(ConnectionType type) { return false; }
    @Override public int getX() { return 0; }
    @Override public int getY() { return 0; }
    @Override public int getZ() { return 0; }
    @Override public Level getWorld() { return null; }
    @Override public boolean isRouterInitialized() { return false; }
    @Override public boolean isRoutingPipe() { return false; }
    @Override public CoreRoutedPipe getRoutingPipe() { return null; }
    @Override public BlockEntity getNextConnectedTile(Direction direction) { return null; }
    @Override public boolean isFirewallPipe() { return false; }
    @Override public IFilter getFirewallFilter() { return null; }
    @Override public BlockEntity getTile() { return null; }
    @Override public boolean divideNetwork() { return false; }
    @Override public boolean powerOnly() { return false; }
    @Override public boolean isOnewayPipe() { return false; }
    @Override public boolean isOutputClosed(Direction direction) { return false; }
    @Override public boolean canConnect(BlockEntity to, Direction direction, boolean flag) { return false; }
    @Override public double getDistance() { return 0; }
    @Override public double getDistanceWeight() { return 0; }
    @Override public boolean isItemPipe() { return false; }
    @Override public boolean isFluidPipe() { return false; }
    @Override public boolean isPowerPipe() { return false; }
    @Override public double getDistanceTo(int dest, Direction ignore, ItemIdentifier ident, boolean isActive, double travelled, double max, List<DoubleCoordinates> visited) { return 0; }
    @Override public boolean acceptItem(LPTravelingItem item, BlockEntity from) { return false; }
    @Override public void refreshTileCacheOnSide(Direction side) {}
    @Override public boolean isMultiBlock() { return false; }
    @Override public Stream<BlockEntity> getPartsOfPipe() { return Stream.empty(); }
}
