package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * A pipe support foot: a face, plus whether it sits upright or sideways.
 * Names the {@code Support_<dir>_<U|S>} groups.
 *
 * <p>Extracted from {@code LogisticsNewRenderPipe.PipeSupport}.</p>
 */
public enum PipeSupport {

    UP_UP(Direction.UP, Orientation.UP_DOWN),
    UP_SIDE(Direction.UP, Orientation.SIDE),
    DOWN_UP(Direction.DOWN, Orientation.UP_DOWN),
    DOWN_SIDE(Direction.DOWN, Orientation.SIDE),
    NORTH_UP(Direction.NORTH, Orientation.UP_DOWN),
    NORTH_SIDE(Direction.NORTH, Orientation.SIDE),
    SOUTH_UP(Direction.SOUTH, Orientation.UP_DOWN),
    SOUTH_SIDE(Direction.SOUTH, Orientation.SIDE),
    EAST_UP(Direction.EAST, Orientation.UP_DOWN),
    EAST_SIDE(Direction.EAST, Orientation.SIDE),
    WEST_UP(Direction.WEST, Orientation.UP_DOWN),
    WEST_SIDE(Direction.WEST, Orientation.SIDE);

    public enum Orientation {
        UP_DOWN("U"),
        SIDE("S");

        public final String letter;

        Orientation(String letter) {
            this.letter = letter;
        }
    }

    public final Direction dir;
    public final Orientation ori;

    PipeSupport(Direction dir, Orientation ori) {
        this.dir = dir;
        this.ori = ori;
    }

    public String groupName() {
        return "Support_" + PipeDirections.letter(dir) + "_" + ori.letter;
    }
}
