package logisticspipes.api.connection;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;

@Getter
public abstract class NeighborBlockEntity<T extends BlockEntity> {

    private final T blockEntity;
    private final Direction direction;

    protected NeighborBlockEntity(T blockEntity, Direction direction) {
        this.blockEntity = blockEntity;
        this.direction = direction;
    }

    public Direction getOurDirection() {
        return direction.getOpposite();
    }

    public abstract boolean isLogisticsPipe();

    public abstract boolean canHandleItems();

    public abstract boolean canHandleFluids();
}
