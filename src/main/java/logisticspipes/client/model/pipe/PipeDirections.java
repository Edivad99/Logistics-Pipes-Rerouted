package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * The single-letter direction naming used by the group names inside LP's OBJ files
 * (e.g. {@code Side_N}, {@code Mount_W_U}, {@code Corner_M_D_NE}).
 *
 * <p>Was {@code LogisticsNewRenderPipe.getDirAsString_Type1}.</p>
 */
public final class PipeDirections {

    private PipeDirections() {
    }

    public static String letter(Direction dir) {
        return switch (dir) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            case UP -> "U";
            case DOWN -> "D";
        };
    }
}
