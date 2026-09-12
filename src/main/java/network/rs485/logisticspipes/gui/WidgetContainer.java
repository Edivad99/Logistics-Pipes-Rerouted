/*
 * Copyright (c) 2022  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2022  RS485
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

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.util.IRectangle;
import network.rs485.logisticspipes.gui.widget.LPGuiWidget;

@Getter
public abstract class WidgetContainer extends LPGuiWidget implements MouseHoverable {

    private final List<LPGuiWidget> children;

    @Setter
    private int gap;

    protected WidgetContainer(List<LPGuiWidget> children, @Nullable Drawable parent, @Nullable Margin margin,
        int gap) {
        super(parent != null ? parent : Screen.INSTANCE, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
            Size.GROW, Size.GROW, margin != null ? margin : Margin.NONE);
        this.children = children;
        this.gap = gap;
    }

    @Override
    public <T extends Drawable> T createChild(java.util.function.Supplier<T> childGetter) {
        if (LogisticsPipes.isDEBUG()) {
            LogisticsPipes.LOG.warn(
                "createChild called on WidgetContainer, but WidgetContainer does not support lazy child creation");
            new Throwable().printStackTrace();
        }
        return childGetter.get();
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        super.draw(guiGraphics, mouseX, mouseY, delta, visibleArea);
        for (LPGuiWidget child : children) {
            child.draw(guiGraphics, mouseX, mouseY, delta, visibleArea);
        }
    }

    /**
     * Places the children in their final positions. The container's size should be defined before
     * running this; afterwards {@link #getWidth()} and {@link #getHeight()} hold the container's size.
     */
    public abstract void placeChildren();

    public @Nullable MouseHoverable getHovered(float mouseX, float mouseY) {
        for (LPGuiWidget child : children) {
            if (child instanceof WidgetContainer container) {
                MouseHoverable hovered = container.getHovered(mouseX, mouseY);
                if (hovered != null) {
                    return hovered;
                }
            } else if (child instanceof MouseHoverable hoverable && hoverable.isMouseHovering(mouseX, mouseY)) {
                return hoverable;
            }
        }
        return null;
    }

    /** The children that asked to grow on either axis. */
    protected List<LPGuiWidget> growableChildren() {
        return children.stream()
            .filter(child -> child.getXSize() == Size.GROW || child.getYSize() == Size.GROW)
            .toList();
    }
}
