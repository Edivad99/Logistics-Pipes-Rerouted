package logisticspipes.api.gui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public final class Margin {

    public static final Margin NONE = new Margin();
    public static final Margin DEFAULT = new Margin(6);

    private final int top;
    private final int left;
    private final int bottom;
    private final int right;

    private final int horizontal;
    private final int vertical;

    public Margin(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.horizontal = left + right;
        this.vertical = top + bottom;
    }

    public Margin() {
        this(0, 0, 0, 0);
    }

    /** The same margin on all four sides. */
    public Margin(int margin) {
        this(margin, margin, margin, margin);
    }

    /** Only the vertical sides. */
    public static Margin vertical(int top, int bottom) {
        return new Margin(top, 0, bottom, 0);
    }

    /** Only the horizontal sides. */
    public static Margin horizontal(int left, int right) {
        return new Margin(0, left, 0, right);
    }

    public int getTop() {
        return top;
    }

    public int getLeft() {
        return left;
    }

    public int getBottom() {
        return bottom;
    }

    public int getRight() {
        return right;
    }

    public int getHorizontal() {
        return horizontal;
    }

    public int getVertical() {
        return vertical;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Margin that
            && top == that.top && left == that.left && bottom == that.bottom && right == that.right;
    }

    @Override
    public int hashCode() {
        return Objects.hash(top, left, bottom, right);
    }

    @Override
    public String toString() {
        return "Margin(top=" + top + ", left=" + left + ", bottom=" + bottom + ", right=" + right + ")";
    }
}
