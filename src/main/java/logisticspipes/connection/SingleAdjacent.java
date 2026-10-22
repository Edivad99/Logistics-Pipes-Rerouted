package logisticspipes.connection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class SingleAdjacent implements Adjacent {

    private final CoreRoutedPipe parent;
    @Getter
    private final Direction dir;
    private final ConnectionType adjacentType;

    public SingleAdjacent(CoreRoutedPipe parent, Direction dir, ConnectionType adjacentType) {
        this.parent = parent;
        this.dir = dir;
        this.adjacentType = adjacentType;
    }

    private @Nullable BlockEntity neighborBlockEntity() {
        Level level = parent.getLevel();
        return level == null ? null : level.getBlockEntity(parent.getPos().relative(dir));
    }

    @Override
    public Map<BlockPos, ConnectionType> connectedPos() {
        return Map.of(parent.getPos().relative(dir), adjacentType);
    }

    @Override
    public @Nullable ConnectionType get(Direction direction) {
        return dir == direction ? adjacentType : null;
    }

    @Override
    public Optional<ConnectionType> optionalGet(Direction direction) {
        return dir == direction ? Optional.of(adjacentType) : Optional.empty();
    }

    @Override
    public Map<NeighborBlockEntity<BlockEntity>, ConnectionType> neighbors() {
        BlockEntity blockEntity = neighborBlockEntity();
        return blockEntity == null
            ? Collections.emptyMap()
            : Map.of(new LPNeighborBlockEntity<>(blockEntity, dir), adjacentType);
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> inventories() {
        if (!adjacentType.isItem()) {
            return Collections.emptyList();
        }
        BlockEntity blockEntity = neighborBlockEntity();
        if (blockEntity == null) {
            return Collections.emptyList();
        }
        LPNeighborBlockEntity<BlockEntity> neighbor = new LPNeighborBlockEntity<>(blockEntity, dir);
        return neighbor.canHandleItems() ? List.of(neighbor) : Collections.emptyList();
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> fluidTanks() {
        if (!adjacentType.isFluid()) {
            return Collections.emptyList();
        }
        BlockEntity blockEntity = neighborBlockEntity();
        if (blockEntity == null) {
            return Collections.emptyList();
        }
        LPNeighborBlockEntity<BlockEntity> neighbor = new LPNeighborBlockEntity<>(blockEntity, dir);
        return neighbor.canHandleFluids() ? List.of(neighbor) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "SingleAdjacent(" + dir.getName() + ": " + adjacentType + ")";
    }
}
