package logisticspipes.utils.math;

import org.jetbrains.annotations.Contract;

import logisticspipes.api.util.IRectangle;

public class MutableRectangle implements IRectangle {

    private float x;
    private float y;
    private float width;
    private float height;

    public MutableRectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Default constructor, sets all fields to 0.
     */
    public MutableRectangle() {
        this(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public MutableRectangle(int x, int y, int width, int height) {
        this((float) x, (float) y, (float) width, (float) height);
    }

    /**
     * Creates rectangle at "origin" with set width and height.
     *
     * @param width  rectangle's width.
     * @param height rectangle's height.
     */
    public MutableRectangle(int width, int height) {
        this(0.0f, 0.0f, (float) width, (float) height);
    }

    /**
     * From two points, the first being the top-left and the second the bottom-right corner. The
     * exact corner that each point represents is not enforced, but some things might break if it
     * does not match this.
     */
    public static MutableRectangle between(float x0, float y0, float x1, float y1) {
        return new MutableRectangle(x0, y0, x1 - x0, y1 - y0);
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
    public float getX0() {
        return x;
    }

    @Override
    public float getY0() {
        return y;
    }

    // Transformations

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle setSize(float newWidth, float newHeight) {
        width = newWidth;
        height = newHeight;
        return this;
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle setSize(int newWidth, int newHeight) {
        return setSize((float) newWidth, (float) newHeight);
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle grow(float growX, float growY) {
        width += growX;
        height += growY;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle grow(float grow) {
        return grow(grow, grow);
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle grow(int growX, int growY) {
        return grow((float) growX, (float) growY);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle grow(int grow) {
        return grow(grow, grow);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle scaleSize(float multiplier) {
        width *= multiplier;
        height *= multiplier;
        return this;
    }

    // Translations

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle setPos(float newX, float newY) {
        x = newX;
        y = newY;
        return this;
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle setPos(int newX, int newY) {
        return setPos((float) newX, (float) newY);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle scalePos(float multiplier) {
        x *= multiplier;
        y *= multiplier;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle translate(float translate) {
        return translate(translate, translate);
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle translate(float translateX, float translateY) {
        x += translateX;
        y += translateY;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle translate(int translate) {
        return translate(translate, translate);
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public MutableRectangle translate(int translateX, int translateY) {
        x += translateX;
        y += translateY;
        return this;
    }

    // Non-destructive

    @Override
    public MutableRectangle translated(float translateX, float translateY) {
        return copy().translate(translateX, translateY);
    }

    @Override
    public MutableRectangle translated(int translateX, int translateY) {
        return copy().translate(translateX, translateY);
    }

    // Both

    @Contract(value = "_ -> this", mutates = "this")
    public MutableRectangle scale(float multiplier) {
        return scalePos(multiplier).scaleSize(multiplier);
    }

    // Non-destructive

    @Override
    public MutableRectangle scaled(float multiplier) {
        return copy().scalePos(multiplier).scaleSize(multiplier);
    }

    // Operations

    @Override
    public MutableRectangle overlap(IRectangle rect) {
        return between(
            Math.max(x, rect.getX0()), Math.max(y, rect.getY0()),
            Math.min(getX1(), rect.getX1()), Math.min(getY1(), rect.getY1()));
    }

    public MutableRectangle copy() {
        return new MutableRectangle(x, y, width, height);
    }

    @Override
    public String toString() {
        return "Rectangle(x = " + x + ", y = " + y + ", width = " + width + ", height = " + height + ")";
    }
}
