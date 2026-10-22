package logisticspipes.connection;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public class LPNeighborBlockEntity<T extends BlockEntity> extends NeighborBlockEntity<T> {

    public LPNeighborBlockEntity(T blockEntity, Direction direction) {
        super(blockEntity, direction);
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
