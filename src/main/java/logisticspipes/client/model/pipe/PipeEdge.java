package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * One of the twelve edges of the pipe frame, as the pair of faces it joins.
 *
 * <p>Extracted from {@code LogisticsNewRenderPipe.Edge}. Two constants are renamed: the
 * original called the south-east and south-west edges of the middle ring
 * {@code Lower_South_East} / {@code Lower_South_West} even though they are neither lower
 * nor paired with a vertical face. The constant names are only ever used in error text, and
 * {@link #groupName()} derives the OBJ name from the two faces, so nothing depends on
 * them.</p>
 */
public enum PipeEdge {

    UPPER_NORTH(Direction.UP, Direction.NORTH),
    UPPER_SOUTH(Direction.UP, Direction.SOUTH),
    UPPER_EAST(Direction.UP, Direction.EAST),
    UPPER_WEST(Direction.UP, Direction.WEST),
    LOWER_NORTH(Direction.DOWN, Direction.NORTH),
    LOWER_SOUTH(Direction.DOWN, Direction.SOUTH),
    LOWER_EAST(Direction.DOWN, Direction.EAST),
    LOWER_WEST(Direction.DOWN, Direction.WEST),
    MIDDLE_NORTH_WEST(Direction.NORTH, Direction.WEST),
    MIDDLE_NORTH_EAST(Direction.NORTH, Direction.EAST),
    MIDDLE_SOUTH_EAST(Direction.SOUTH, Direction.EAST),
    MIDDLE_SOUTH_WEST(Direction.SOUTH, Direction.WEST);

    public final Direction part1;
    public final Direction part2;

    PipeEdge(Direction part1, Direction part2) {
        this.part1 = part1;
        this.part2 = part2;
    }

    /**
     * The OBJ group name for this edge. Up/down edges are named {@code Edge_M_<a>_<b>},
     * the middle ring {@code Edge_M_S_<a><b>}.
     */
    public String groupName() {
        if (part1 == Direction.UP || part1 == Direction.DOWN) {
            return "Edge_M_" + PipeDirections.letter(part1) + "_" + PipeDirections.letter(part2);
        }
        return "Edge_M_S_" + PipeDirections.letter(part1) + PipeDirections.letter(part2);
    }
}
