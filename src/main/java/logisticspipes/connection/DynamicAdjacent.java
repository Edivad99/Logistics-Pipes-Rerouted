package logisticspipes.connection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class DynamicAdjacent implements Adjacent {

    private final CoreRoutedPipe parent;

    /** Indexed by {@link Direction#get3DDataValue()}; a null entry means "not connected on that side". */
    private final @Nullable ConnectionType[] cache;

    public DynamicAdjacent(CoreRoutedPipe parent, @Nullable ConnectionType[] cache) {
        this.parent = parent;
        this.cache = cache;
    }

    private @Nullable BlockEntity neighborBlockEntity(Direction direction) {
        Level level = parent.getLevel();
        return level == null ? null : level.getBlockEntity(parent.getPos().relative(direction));
    }

    /**
     * The neighbours whose connection type passes the filter and that accept the matching resource.
     */
    private List<NeighborBlockEntity<BlockEntity>> neighborsMatching(
        Predicate<ConnectionType> typeFilter,
        Predicate<LPNeighborBlockEntity<BlockEntity>> canHandle) {
        List<NeighborBlockEntity<BlockEntity>> result = new ArrayList<>();
        for (int index = 0; index < cache.length; index++) {
            ConnectionType connectionType = cache[index];
            if (connectionType == null || !typeFilter.test(connectionType)) {
                continue;
            }
            Direction direction = Direction.from3DDataValue(index);
            BlockEntity blockEntity = neighborBlockEntity(direction);
            if (blockEntity == null) {
                continue;
            }
            LPNeighborBlockEntity<BlockEntity> neighbor = new LPNeighborBlockEntity<>(blockEntity, direction);
            if (canHandle.test(neighbor)) {
                result.add(neighbor);
            }
        }
        return result;
    }

    @Override
    public Map<BlockPos, ConnectionType> connectedPos() {
        Map<BlockPos, ConnectionType> result = new LinkedHashMap<>();
        for (int index = 0; index < cache.length; index++) {
            ConnectionType connectionType = cache[index];
            if (connectionType != null) {
                result.put(parent.getPos().relative(Direction.from3DDataValue(index)), connectionType);
            }
        }
        return result;
    }

    @Override
    public @Nullable ConnectionType get(Direction direction) {
        return cache[direction.get3DDataValue()];
    }

    @Override
    public Optional<ConnectionType> optionalGet(Direction direction) {
        return Optional.ofNullable(cache[direction.get3DDataValue()]);
    }

    @Override
    public Map<NeighborBlockEntity<BlockEntity>, ConnectionType> neighbors() {
        Map<NeighborBlockEntity<BlockEntity>, ConnectionType> result = new LinkedHashMap<>();
        for (int index = 0; index < cache.length; index++) {
            ConnectionType connectionType = cache[index];
            if (connectionType == null) {
                continue;
            }
            Direction direction = Direction.from3DDataValue(index);
            BlockEntity blockEntity = neighborBlockEntity(direction);
            if (blockEntity != null) {
                result.put(new LPNeighborBlockEntity<>(blockEntity, direction), connectionType);
            }
        }
        return result;
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> inventories() {
        return neighborsMatching(ConnectionType::isItem, LPNeighborBlockEntity::canHandleItems);
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> fluidTanks() {
        return neighborsMatching(ConnectionType::isFluid, LPNeighborBlockEntity::canHandleFluids);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "DynamicAdjacent(", ")");
        for (Direction direction : Direction.values()) {
            joiner.add("{" + direction.getName() + ": " + cache[direction.get3DDataValue()] + "}");
        }
        return joiner.toString();
    }
}
