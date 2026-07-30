package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * A mount bracket: the face it belongs to, and which neighbouring face it leans against.
 * Names the {@code Mount_<dir>_<side>} and {@code Texture_Connector_<dir>_<side>} groups.
 *
 * <p>Extracted from {@code LogisticsNewRenderPipe.PipeMount}.</p>
 */
public enum PipeMount {

    UP_NORTH(Direction.UP, Direction.NORTH),
    UP_SOUTH(Direction.UP, Direction.SOUTH),
    UP_EAST(Direction.UP, Direction.EAST),
    UP_WEST(Direction.UP, Direction.WEST),
    DOWN_NORTH(Direction.DOWN, Direction.NORTH),
    DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
    DOWN_EAST(Direction.DOWN, Direction.EAST),
    DOWN_WEST(Direction.DOWN, Direction.WEST),
    NORTH_UP(Direction.NORTH, Direction.UP),
    NORTH_DOWN(Direction.NORTH, Direction.DOWN),
    NORTH_EAST(Direction.NORTH, Direction.EAST),
    NORTH_WEST(Direction.NORTH, Direction.WEST),
    SOUTH_UP(Direction.SOUTH, Direction.UP),
    SOUTH_DOWN(Direction.SOUTH, Direction.DOWN),
    SOUTH_EAST(Direction.SOUTH, Direction.EAST),
    SOUTH_WEST(Direction.SOUTH, Direction.WEST),
    EAST_UP(Direction.EAST, Direction.UP),
    EAST_DOWN(Direction.EAST, Direction.DOWN),
    EAST_NORTH(Direction.EAST, Direction.NORTH),
    EAST_SOUTH(Direction.EAST, Direction.SOUTH),
    WEST_UP(Direction.WEST, Direction.UP),
    WEST_DOWN(Direction.WEST, Direction.DOWN),
    WEST_NORTH(Direction.WEST, Direction.NORTH),
    WEST_SOUTH(Direction.WEST, Direction.SOUTH);

    public final Direction dir;
    public final Direction side;

    PipeMount(Direction dir, Direction side) {
        this.dir = dir;
        this.side = side;
    }

    public String groupName() {
        return "Mount_" + PipeDirections.letter(dir) + "_" + PipeDirections.letter(side);
    }

    public String connectorGroupName() {
        return "Texture_Connector_" + PipeDirections.letter(dir) + "_" + PipeDirections.letter(side);
    }
}
