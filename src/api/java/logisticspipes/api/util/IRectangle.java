package logisticspipes.api.util;

public interface IRectangle {

    // Size getters
    float getWidth();

    float getHeight();

    // Position getters
    float getX0();

    float getY0();

    default float getX1() {
        return getX0() + getWidth();
    }

    default float getY1() {
        return getY0() + getHeight();
    }

    // Named getters
    default float getLeft() {
        return getX0();
    }

    default float getRight() {
        return getX1();
    }

    default float getTop() {
        return getY0();
    }

    default float getBottom() {
        return getY1();
    }

    // Integer getters, only to be used when precision is not important
    default int getRoundedX() {
        return Math.round(getX0());
    }

    default int getRoundedY() {
        return Math.round(getY0());
    }

    default int getRoundedWidth() {
        return Math.round(getWidth());
    }

    default int getRoundedHeight() {
        return Math.round(getHeight());
    }

    default int getRoundedLeft() {
        return getRoundedX();
    }

    default int getRoundedRight() {
        return Math.round(getRight());
    }

    default int getRoundedTop() {
        return getRoundedY();
    }

    default int getRoundedBottom() {
        return Math.round(getBottom());
    }

    // Non-destructive
    IRectangle translated(float translateX, float translateY);

    IRectangle translated(int translateX, int translateY);

    default IRectangle translated(IRectangle rect) {
        return translated(rect.getX0(), rect.getY0());
    }

    // Non-destructive
    IRectangle scaled(float multiplier);

    // Logic checks
    default boolean contains(float x, float y) {
        return x >= getX0() && x <= getX1() && y >= getY0() && y <= getY1();
    }

    default boolean contains(int x, int y) {
        return contains((float) x, (float) y);
    }

    default boolean contains(IRectangle rect) {
        return contains(rect.getX0(), rect.getY0()) && contains(rect.getX1(), rect.getY1());
    }

    default boolean intersects(IRectangle rect) {
        return !(getRight() < rect.getLeft() || rect.getRight() < getLeft()
            || getBottom() < rect.getTop() || rect.getBottom() < getTop());
    }

    // Operations
    IRectangle overlap(IRectangle rect);
}
