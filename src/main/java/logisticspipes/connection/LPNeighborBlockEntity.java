package logisticspipes.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public class LPNeighborBlockEntity<T extends BlockEntity> extends NeighborBlockEntity<T> {

    public LPNeighborBlockEntity(T blockEntity, Direction direction) {
        super(blockEntity, direction);
    }

    public static List<LPNeighborBlockEntity<BlockEntity>> allAround(BlockEntity center) {
        final Level level = Objects.requireNonNull(center.getLevel());
        final List<LPNeighborBlockEntity<BlockEntity>> neighbors = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(center.getBlockPos().relative(direction));
            if (blockEntity != null) {
                neighbors.add(new LPNeighborBlockEntity<>(blockEntity, direction));
            }
        }
        return neighbors;
    }

    public static List<LPNeighborBlockEntity<BlockEntity>> connectedTo(BlockEntity pipe) {
        if (PipeInformationManager.isNotAPipe(pipe)) {
            LogisticsPipes.LOG.warn("The coordinates didn't hold a pipe at all", new Throwable("Stack trace"));
            return List.of();
        }
        return allAround(pipe).stream()
            .filter(
                adjacent -> PipeInformationManager.canConnect(pipe, adjacent.getBlockEntity(), adjacent.getDirection()))
            .toList();
    }

    @Override
    public boolean isLogisticsPipe() {
        return getBlockEntity() instanceof LogisticsGenericPipeBlockEntity;
    }

    @Override
    public boolean canHandleItems() {
        return NeighborBlockEntityUtil.getInventoryUtil(this) != null;
    }

    @Override
    public boolean canHandleFluids() {
        return NeighborBlockEntityUtil.getTankUtil(this) != null;
    }
}
