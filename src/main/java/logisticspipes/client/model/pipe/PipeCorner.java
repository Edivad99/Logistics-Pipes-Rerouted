package logisticspipes.client.model.pipe;

import net.minecraft.core.Direction;

/**
 * One of the eight corners of the pipe frame, named by its up/down, north/south and
 * east/west components. The component letters spell the OBJ group names, e.g.
 * {@code Corner_M_D_NE} for {@link #DOWN_NORTH_EAST}.
 *
 * <p>Extracted from {@code LogisticsNewRenderPipe} so the baked-model pipeline can name
 * pipe parts without depending on the legacy immediate-mode renderer.</p>
 */
public enum PipeCorner {

    UP_NORTH_WEST(UpDown.UP, NorthSouth.NORTH, EastWest.WEST),
    UP_NORTH_EAST(UpDown.UP, NorthSouth.NORTH, EastWest.EAST),
    UP_SOUTH_WEST(UpDown.UP, NorthSouth.SOUTH, EastWest.WEST),
    UP_SOUTH_EAST(UpDown.UP, NorthSouth.SOUTH, EastWest.EAST),
    DOWN_NORTH_WEST(UpDown.DOWN, NorthSouth.NORTH, EastWest.WEST),
    DOWN_NORTH_EAST(UpDown.DOWN, NorthSouth.NORTH, EastWest.EAST),
    DOWN_SOUTH_WEST(UpDown.DOWN, NorthSouth.SOUTH, EastWest.WEST),
    DOWN_SOUTH_EAST(UpDown.DOWN, NorthSouth.SOUTH, EastWest.EAST);

    public enum UpDown {
        UP("U", Direction.UP),
        DOWN("D", Direction.DOWN);

        public final String letter;
        public final Direction dir;

        UpDown(String letter, Direction dir) {
            this.letter = letter;
            this.dir = dir;
        }
    }

    public enum NorthSouth {
        NORTH("N", Direction.NORTH),
        SOUTH("S", Direction.SOUTH);

        public final String letter;
        public final Direction dir;

        NorthSouth(String letter, Direction dir) {
            this.letter = letter;
            this.dir = dir;
        }
    }

    public enum EastWest {
        EAST("E", Direction.EAST),
        WEST("W", Direction.WEST);

        public final String letter;
        public final Direction dir;

        EastWest(String letter, Direction dir) {
            this.letter = letter;
            this.dir = dir;
        }
    }

    public final UpDown ud;
    public final NorthSouth ns;
    public final EastWest ew;

    PipeCorner(UpDown ud, NorthSouth ns, EastWest ew) {
        this.ud = ud;
        this.ns = ns;
        this.ew = ew;
    }

    /**
     * The corner's OBJ name suffix, e.g. {@code D_NE} — the part after {@code Corner_M_}.
     */
    public String groupSuffix() {
        return ud.letter + "_" + ns.letter + ew.letter;
    }
}
