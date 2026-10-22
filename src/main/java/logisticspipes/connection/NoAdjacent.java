package logisticspipes.connection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.connection.NeighborBlockEntity;

public final class NoAdjacent implements Adjacent {

    public static final NoAdjacent INSTANCE = new NoAdjacent();

    private NoAdjacent() {
    }

    @Override
    public Map<BlockPos, ConnectionType> connectedPos() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable ConnectionType get(Direction direction) {
        return null;
    }

    @Override
    public Optional<ConnectionType> optionalGet(Direction direction) {
        return Optional.empty();
    }

    @Override
    public Map<NeighborBlockEntity<BlockEntity>, ConnectionType> neighbors() {
        return Collections.emptyMap();
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> inventories() {
        return Collections.emptyList();
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> fluidTanks() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "NoAdjacent";
    }
}
