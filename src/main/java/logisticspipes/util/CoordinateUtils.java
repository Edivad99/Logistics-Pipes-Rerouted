package logisticspipes.util;

import net.minecraft.core.Direction;

public final class CoordinateUtils {

    private CoordinateUtils() {
    }

    public static DoubleCoordinates add(DoubleCoordinates coords, Direction direction) {
        coords.setXCoord(coords.getXCoord() + direction.getStepX());
        coords.setYCoord(coords.getYCoord() + direction.getStepY());
        coords.setZCoord(coords.getZCoord() + direction.getStepZ());
        return coords;
    }

    public static DoubleCoordinates add(DoubleCoordinates coords, Direction direction, double times) {
        coords.setXCoord(coords.getXCoord() + direction.getStepX() * times);
        coords.setYCoord(coords.getYCoord() + direction.getStepY() * times);
        coords.setZCoord(coords.getZCoord() + direction.getStepZ() * times);
        return coords;
    }

    public static DoubleCoordinates sum(DoubleCoordinates coords, Direction direction) {
        DoubleCoordinates ret = new DoubleCoordinates(coords);
        return CoordinateUtils.add(ret, direction);
    }

}
