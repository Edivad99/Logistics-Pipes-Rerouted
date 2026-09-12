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

package network.rs485.logisticspipes.util.math;

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

    public static MutableRectangle fromRectangle(IRectangle rect) {
        return new MutableRectangle(rect.getX0(), rect.getY0(), rect.getWidth(), rect.getHeight());
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
    public MutableRectangle setSizeFromRectangle(IRectangle rect) {
        return setSize(rect.getWidth(), rect.getHeight());
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
    public MutableRectangle setPosFromRectangle(IRectangle rect) {
        return setPos(rect.getX0(), rect.getY0());
    }

    @Contract(value = "-> this", mutates = "this")
    public MutableRectangle resetPos() {
        return setPos(0.0f, 0.0f);
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
