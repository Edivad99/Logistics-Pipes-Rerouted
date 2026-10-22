package logisticspipes.connection;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ISlotUpgradeManager;

public class LPNeighborBlockEntitySneakyInsertion<T extends BlockEntity> extends LPNeighborBlockEntity<T> {

    private Direction sneakyDirection;

    public LPNeighborBlockEntitySneakyInsertion(T blockEntity, Direction direction) {
        super(blockEntity, direction);
        this.sneakyDirection = super.getOurDirection();
    }

    @Override
    public Direction getOurDirection() {
        return sneakyDirection;
    }

    public LPNeighborBlockEntitySneakyInsertion<T> from(Direction direction) {
        sneakyDirection = direction;
        return this;
    }

    public LPNeighborBlockEntitySneakyInsertion<T> from(@Nullable ISlotUpgradeManager upgradeManager) {
        // Keeps the pipe's own side when the upgrade is there but no side was picked: the manager
        // reports a sneaky upgrade before the player configures one.
        if (upgradeManager != null && upgradeManager.hasSneakyUpgrade()) {
            Direction orientation = upgradeManager.getSneakyOrientation();
            if (orientation != null) {
                sneakyDirection = orientation;
            }
        }
        return this;
    }
}
