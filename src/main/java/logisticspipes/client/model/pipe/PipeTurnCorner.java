package logisticspipes.client.model.pipe;

import java.util.List;

import net.minecraft.core.Direction;

/**
 * A corner piece together with the axis it turns along — the 8 corners × 3 turns that make
 * up the {@code Corner_I_*} and {@code Spacer*} groups.
 *
 * <p>Extracted from {@code LogisticsNewRenderPipe.PipeTurnCorner}. The {@link #number} is
 * the spacer index baked into the OBJ group names and is not derivable from the corner and
 * turn — it comes from how the model was authored.</p>
 */
public enum PipeTurnCorner {

    UP_NORTH_WEST_TURN_NORTH_SOUTH(PipeCorner.UP_NORTH_WEST, PipeTurn.NORTH_SOUTH, 1),
    UP_NORTH_WEST_TURN_EAST_WEST(PipeCorner.UP_NORTH_WEST, PipeTurn.EAST_WEST, 14),
    UP_NORTH_WEST_TURN_UP_DOWN(PipeCorner.UP_NORTH_WEST, PipeTurn.UP_DOWN, 23),
    UP_NORTH_EAST_TURN_NORTH_SOUTH(PipeCorner.UP_NORTH_EAST, PipeTurn.NORTH_SOUTH, 2),
    UP_NORTH_EAST_TURN_EAST_WEST(PipeCorner.UP_NORTH_EAST, PipeTurn.EAST_WEST, 9),
    UP_NORTH_EAST_TURN_UP_DOWN(PipeCorner.UP_NORTH_EAST, PipeTurn.UP_DOWN, 22),
    UP_SOUTH_WEST_TURN_NORTH_SOUTH(PipeCorner.UP_SOUTH_WEST, PipeTurn.NORTH_SOUTH, 6),
    UP_SOUTH_WEST_TURN_EAST_WEST(PipeCorner.UP_SOUTH_WEST, PipeTurn.EAST_WEST, 13),
    UP_SOUTH_WEST_TURN_UP_DOWN(PipeCorner.UP_SOUTH_WEST, PipeTurn.UP_DOWN, 24),
    UP_SOUTH_EAST_TURN_NORTH_SOUTH(PipeCorner.UP_SOUTH_EAST, PipeTurn.NORTH_SOUTH, 5),
    UP_SOUTH_EAST_TURN_EAST_WEST(PipeCorner.UP_SOUTH_EAST, PipeTurn.EAST_WEST, 10),
    UP_SOUTH_EAST_TURN_UP_DOWN(PipeCorner.UP_SOUTH_EAST, PipeTurn.UP_DOWN, 21),
    DOWN_NORTH_WEST_TURN_NORTH_SOUTH(PipeCorner.DOWN_NORTH_WEST, PipeTurn.NORTH_SOUTH, 4),
    DOWN_NORTH_WEST_TURN_EAST_WEST(PipeCorner.DOWN_NORTH_WEST, PipeTurn.EAST_WEST, 15),
    DOWN_NORTH_WEST_TURN_UP_DOWN(PipeCorner.DOWN_NORTH_WEST, PipeTurn.UP_DOWN, 20),
    DOWN_NORTH_EAST_TURN_NORTH_SOUTH(PipeCorner.DOWN_NORTH_EAST, PipeTurn.NORTH_SOUTH, 3),
    DOWN_NORTH_EAST_TURN_EAST_WEST(PipeCorner.DOWN_NORTH_EAST, PipeTurn.EAST_WEST, 12),
    DOWN_NORTH_EAST_TURN_UP_DOWN(PipeCorner.DOWN_NORTH_EAST, PipeTurn.UP_DOWN, 17),
    DOWN_SOUTH_WEST_TURN_NORTH_SOUTH(PipeCorner.DOWN_SOUTH_WEST, PipeTurn.NORTH_SOUTH, 7),
    DOWN_SOUTH_WEST_TURN_EAST_WEST(PipeCorner.DOWN_SOUTH_WEST, PipeTurn.EAST_WEST, 16),
    DOWN_SOUTH_WEST_TURN_UP_DOWN(PipeCorner.DOWN_SOUTH_WEST, PipeTurn.UP_DOWN, 19),
    DOWN_SOUTH_EAST_TURN_NORTH_SOUTH(PipeCorner.DOWN_SOUTH_EAST, PipeTurn.NORTH_SOUTH, 8),
    DOWN_SOUTH_EAST_TURN_EAST_WEST(PipeCorner.DOWN_SOUTH_EAST, PipeTurn.EAST_WEST, 11),
    DOWN_SOUTH_EAST_TURN_UP_DOWN(PipeCorner.DOWN_SOUTH_EAST, PipeTurn.UP_DOWN, 18);

    public final PipeCorner corner;
    public final PipeTurn turn;
    /**
     * Index of the matching {@code Spacer<n>} group in the OBJ file.
     */
    public final int number;

    PipeTurnCorner(PipeCorner corner, PipeTurn turn, int number) {
        this.corner = corner;
        this.turn = turn;
        this.number = number;
    }

    /**
     * Whichever of the turn's two directions the corner actually reaches.
     */
    public Direction getPointer() {
        List<Direction> candidates = List.of(corner.ew.dir, corner.ns.dir, corner.ud.dir);
        if (candidates.contains(turn.dir1)) {
            return turn.dir1;
        }
        if (candidates.contains(turn.dir2)) {
            return turn.dir2;
        }
        throw new UnsupportedOperationException(name());
    }

    /**
     * The variant suffix distinguishing the three turns that share a corner in the OBJ file:
     * {@code Corner_I_<corner>} for {@link PipeTurn#UP_DOWN}, {@code ...1} for
     * {@link PipeTurn#EAST_WEST} and {@code ...2} for {@link PipeTurn#NORTH_SOUTH}.
     */
    public int groupVariant() {
        return switch (turn) {
            case UP_DOWN -> 0;
            case EAST_WEST -> 1;
            case NORTH_SOUTH -> 2;
        };
    }
}
