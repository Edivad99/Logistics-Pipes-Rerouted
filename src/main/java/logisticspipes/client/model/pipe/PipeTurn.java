package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * The axis a corner piece turns along. Extracted from {@code LogisticsNewRenderPipe.Turn}.
 */
public enum PipeTurn {

    NORTH_SOUTH(Direction.NORTH, Direction.SOUTH),
    EAST_WEST(Direction.EAST, Direction.WEST),
    UP_DOWN(Direction.UP, Direction.DOWN);

    public final Direction dir1;
    public final Direction dir2;

    PipeTurn(Direction dir1, Direction dir2) {
        this.dir1 = dir1;
        this.dir2 = dir2;
    }
}
