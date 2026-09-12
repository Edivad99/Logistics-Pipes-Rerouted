package logisticspipes.api.util;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public final class Rectangle implements IRectangle {

    private final float x0;
    private final float y0;
    private final float width;
    private final float height;

    public Rectangle(float x0, float y0, float width, float height) {
        this.x0 = x0;
        this.y0 = y0;
        this.width = width;
        this.height = height;
    }

    /** At the origin, with the given size. */
    public Rectangle(float width, float height) {
        this(0.0f, 0.0f, width, height);
    }

    public Rectangle(int x, int y, int width, int height) {
        this((float) x, (float) y, (float) width, (float) height);
    }

    public Rectangle(int width, int height) {
        this(0.0f, 0.0f, (float) width, (float) height);
    }

    /**
     * From two points, the first being the top-left and the second the bottom-right corner. The
     * exact corner that each point represents is not enforced, but some things might break if it
     * does not match this.
     */
    public static Rectangle between(float x0, float y0, float x1, float y1) {
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    @Override
    public float getX0() {
        return x0;
    }

    @Override
    public float getY0() {
        return y0;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public Rectangle translated(float translateX, float translateY) {
        return new Rectangle(x0 + translateX, y0 + translateY, width, height);
    }

    @Override
    public Rectangle translated(int translateX, int translateY) {
        return new Rectangle(x0 + translateX, y0 + translateY, width, height);
    }

    @Override
    public Rectangle scaled(float multiplier) {
        return new Rectangle(x0 * multiplier, y0 * multiplier, width * multiplier, height * multiplier);
    }

    @Override
    public Rectangle overlap(IRectangle rect) {
        return between(
            Math.max(x0, rect.getX0()), Math.max(y0, rect.getY0()),
            Math.min(getX1(), rect.getX1()), Math.min(getY1(), rect.getY1()));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Rectangle that
            && Float.compare(x0, that.x0) == 0
            && Float.compare(y0, that.y0) == 0
            && Float.compare(width, that.width) == 0
            && Float.compare(height, that.height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x0, y0, width, height);
    }

    @Override
    public String toString() {
        return "Rectangle(x0=" + x0 + ", y0=" + y0 + ", width=" + width + ", height=" + height + ")";
    }
}
