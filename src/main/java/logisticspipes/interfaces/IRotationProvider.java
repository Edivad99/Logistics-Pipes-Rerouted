package logisticspipes.interfaces;

import net.minecraft.core.Direction;

/**
 * A block or pipe placed facing a direction, which it stores as a rotation number.
 */
public interface IRotationProvider {

    /**
     * @deprecated use {@link #getFacing}
     */
    @Deprecated
    int getRotation();

    default Direction getFacing() {
        return switch (getRotation()) {
            case 0 -> Direction.WEST;
            case 1 -> Direction.EAST;
            case 2 -> Direction.NORTH;
            default -> Direction.SOUTH;
        };
    }

    /**
     * @deprecated use {@link #setFacing}
     */
    @Deprecated
    void setRotation(int rotation);

    /**
     * Stores the rotation for {@code facing}. Up and down have none of their own, and count as south.
     */
    default void setFacing(Direction facing) {
        switch (facing) {
            case NORTH -> setRotation(3);
            case DOWN, UP, SOUTH -> setRotation(2);
            case WEST -> setRotation(1);
            case EAST -> setRotation(0);
        }
    }
}
