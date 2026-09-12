/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.gui;

import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.util.IRectangle;
import logisticspipes.api.util.Rectangle;
import network.rs485.logisticspipes.util.math.MutableRectangle;

public interface Drawable {

    MutableRectangle getRelativeBody();

    @Nullable Drawable getParent();

    void setParent(@Nullable Drawable parent);

    /** Relative x position. */
    default float getX() {
        return getRelativeBody().getX0();
    }

    /** Relative y position. */
    default float getY() {
        return getRelativeBody().getY0();
    }

    /** Drawable's width. */
    default int getWidth() {
        return getRelativeBody().getRoundedWidth();
    }

    /** Drawable's height. */
    default int getHeight() {
        return getRelativeBody().getRoundedHeight();
    }

    /** Absolute left position. */
    default float getLeft() {
        Drawable currentParent = getParent();
        return (currentParent == null ? 0.0f : currentParent.getLeft()) + getX();
    }

    /** Absolute right position. */
    default float getRight() {
        return getLeft() + getWidth();
    }

    /** Absolute top position. */
    default float getTop() {
        Drawable currentParent = getParent();
        return (currentParent == null ? 0.0f : currentParent.getTop()) + getY();
    }

    /** Absolute bottom position. */
    default float getBottom() {
        return getTop() + getHeight();
    }

    /** Absolute drawable body. */
    default Rectangle getAbsoluteBody() {
        return Rectangle.between(getLeft(), getTop(), getRight(), getBottom());
    }

    /**
     * Assigns a new child's parent to this.
     */
    default <T extends Drawable> T createChild(Supplier<T> childGetter) {
        T child = childGetter.get();
        child.setParent(this);
        return child;
    }

    /**
     * This is just like the normal draw functions for minecraft Gui classes but with the added current Y offset.
     *
     * @param mouseX      X position of the mouse (absolute, screen)
     * @param mouseY      Y position of the mouse (absolute, screen)
     * @param delta       Timing floating value
     * @param visibleArea used to avoid draw calls on non-visible children
     */
    default void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        // No default drawing; the debug wireframe went with the guide book.
    }

    /**
     * Updates the Drawable's position by giving it the exact X and Y where it should start.
     *
     * @param x the X position of the Drawable.
     * @param y the Y position of the Drawable.
     */
    default void setPos(int x, int y) {
        getRelativeBody().setPos(x, y);
    }

    /**
     * Checks if the current Drawable is within the vertical constraints of the given area.
     *
     * @param visibleArea Desired visible area to check
     * @return true if within constraints false otherwise.
     */
    default boolean visible(IRectangle visibleArea) {
        return visibleArea.intersects(getAbsoluteBody());
    }
}
