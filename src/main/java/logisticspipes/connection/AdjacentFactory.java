package logisticspipes.connection;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.pathfinder.PipeInformationManager;

public final class AdjacentFactory {

    private AdjacentFactory() {
    }

    public static Adjacent createAdjacentCache(CoreRoutedPipe parent) {
        List<LPNeighborBlockEntity<?>> connected =
            LPNeighborBlockEntity.connectedTo(Objects.requireNonNull(parent.getContainer())).stream()
                .filter(neighbor -> PipeInformationManager.isNotAPipe(neighbor.getBlockEntity())
                    && !parent.isSideBlocked(neighbor.getDirection(), false))
                .<LPNeighborBlockEntity<?>>map(neighbor -> neighbor)
                .toList();

        if (connected.isEmpty()) {
            return NoAdjacent.INSTANCE;
        }
        if (connected.size() == 1) {
            LPNeighborBlockEntity<?> only = connected.get(0);
            return new SingleAdjacent(parent, only.getDirection(), connectionTypeFor(only));
        }
        @Nullable ConnectionType[] cache = new ConnectionType[Direction.values().length];
        for (LPNeighborBlockEntity<?> neighbor : connected) {
            cache[neighbor.getDirection().get3DDataValue()] = connectionTypeFor(neighbor);
        }
        return new DynamicAdjacent(parent, cache);
    }

    private static ConnectionType connectionTypeFor(LPNeighborBlockEntity<?> neighbor) {
        boolean items = neighbor.canHandleItems();
        boolean fluids = neighbor.canHandleFluids();
        if (items && !fluids) {
            return ConnectionType.ITEM;
        }
        if (fluids && !items) {
            return ConnectionType.FLUID;
        }
        return ConnectionType.UNDEFINED;
    }
}
