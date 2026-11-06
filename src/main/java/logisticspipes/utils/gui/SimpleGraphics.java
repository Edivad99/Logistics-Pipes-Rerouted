package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import logisticspipes.utils.Color;

/**
 * Utils class for simple drawing methods.
 */
public final class SimpleGraphics {

    private SimpleGraphics() {
    }

    /**
     * Takes colors as enum values from {@link Color}.
     *
     */
    public static void drawHorizontalLine(GuiGraphicsExtractor guiGraphics, int x1, int x2, int y, Color color,
        int thickness) {
        SimpleGraphics.drawHorizontalLine(guiGraphics, x1, x2, y, Color.getValue(color), thickness);
    }

    /**
     * Draws a horizontal line from x1 to x2.
     *
     * @param x1        the start coordinate
     * @param x2        the end coordinate
     * @param y         the y-coordinate the line is on
     * @param color     the color, which the line will have
     * @param thickness the thickness, which the line will have
     */
    public static void drawHorizontalLine(GuiGraphicsExtractor guiGraphics, int x1, int x2, int y, int color,
        int thickness) {
        if (x2 < x1) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }

        guiGraphics.fill(x1, y, x2 + 1, y + thickness, color);
    }

    /**
     * Takes colors as enum values from {@link Color}.
     *
     */
    public static void drawVerticalLine(GuiGraphicsExtractor guiGraphics, int x, int y1, int y2, Color color,
        int thickness) {
        SimpleGraphics.drawVerticalLine(guiGraphics, x, y1, y2, Color.getValue(color), thickness);
    }

    /**
     * Draws a vertical line from y1 to y2.
     *
     * @param x         the x-coordinate the line is on
     * @param y1        the start coordinate
     * @param y2        the end coordinate
     * @param color     the color, which the line will have
     * @param thickness the thickness, which the line will have
     */
    public static void drawVerticalLine(GuiGraphicsExtractor guiGraphics, int x, int y1, int y2, int color,
        int thickness) {
        if (y2 < y1) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }

        guiGraphics.fill(x, y1 + 1, x + thickness, y2, color);
    }

    /**
     * Takes colors as enum values from {@link Color}.
     *
     * @see #drawRectNoBlend(GuiGraphicsExtractor, int, int, int, int, int, double)
     */
    public static void drawRectNoBlend(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, Color color,
        double zLevel) {
        SimpleGraphics.drawRectNoBlend(guiGraphics, x1, y1, x2, y2, Color.getValue(color), zLevel);
    }

    /**
     * Draws a solid color rectangle with the specified coordinates and color.
     * This variation does not use GL_BLEND.
     *
     * @param x1     the first x-coordinate of the rectangle
     * @param y1     the first y-coordinate of the rectangle
     * @param x2     the second x-coordinate of the rectangle
     * @param y2     the second y-coordinate of the rectangle
     * @param color  the color of the rectangle
     * @param zLevel the z-level of the graphic
     */
    public static void drawRectNoBlend(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color,
        double zLevel) {
        int temp;

        if (x1 < x2) {
            temp = x1;
            x1 = x2;
            x2 = temp;
        }

        if (y1 < y2) {
            temp = y1;
            y1 = y2;
            y2 = temp;
        }

        guiGraphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
    }

    /**
     * Takes colors as enum values from {@link Color}.
     *
     * @see #drawGradientRect(GuiGraphicsExtractor, int, int, int, int, int, int, double)
     */
    public static void drawGradientRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, Color colorA,
        Color colorB, double zLevel) {
        SimpleGraphics.drawGradientRect(guiGraphics, x1, y1, x2, y2, Color.getValue(colorA), Color.getValue(colorB),
            zLevel);
    }

    /**
     * Draws a rectangle with a vertical gradient between the specified colors.
     *
     * @param x1     the first x-coordinate of the rectangle
     * @param y1     the first y-coordinate of the rectangle
     * @param x2     the second x-coordinate of the rectangle
     * @param y2     the second y-coordinate of the rectangle
     * @param colorA the first color, starting from y1
     * @param colorB the second color, ending in y2
     * @param zLevel the z-level of the graphic
     */
    public static void drawGradientRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int colorA,
        int colorB,
        double zLevel) {
        guiGraphics.fillGradient(x1, y1, x2, y2, colorA, colorB);
    }

    /**
     * Takes colors as enum values from {@link Color}.
     *
     * @see #drawQuad(GuiGraphicsExtractor, Object, int, int, int, int, int, double)
     */
    public static void drawQuad(GuiGraphicsExtractor guiGraphics, Object tessellator, int x, int y, int width,
        int height,
        Color color, double zLevel) {
        SimpleGraphics.drawQuad(guiGraphics, tessellator, x, y, width, height, Color.getValue(color), zLevel);
    }

    /**
     * Draws a solid-color rectangle. The {@code tessellator} parameter is kept for
     * source compatibility with 1.12.2 call sites and is ignored; the rectangle is
     * painted via {@link net.minecraft.client.gui.GuiGraphicsExtractor#fill}.
     */
    public static void drawQuad(GuiGraphicsExtractor guiGraphics, Object tessellator, int x, int y, int width,
        int height,
        int color, double zLevel) {
        guiGraphics.fill(x, y, x + width, y + height, color);
    }
}
