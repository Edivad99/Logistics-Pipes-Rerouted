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

import org.jspecify.annotations.Nullable;

import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import network.rs485.logisticspipes.gui.widget.LPGuiWidget;

public class VerticalWidgetContainer extends WidgetContainer {

    public VerticalWidgetContainer(List<LPGuiWidget> children, @Nullable Drawable parent, @Nullable Margin margin,
        int gap) {
        super(children, parent, margin, gap);
    }

    @Override
    public int getMaxWidth() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxHeight() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void initWidget() {
        setSize(getMinWidth(), getMinHeight());
        getChildren().forEach(LPGuiWidget::initWidget);
    }

    private void growChildren() {
        List<LPGuiWidget> canGrow = growableChildren();
        for (LPGuiWidget child : canGrow) {
            if (child.getXSize() == Size.GROW) {
                child.setWidth(getWidth());
            }
            if (child.getYSize() == Size.GROW) {
                child.setHeight(child.getHeight() + (getHeight() - getMinHeight()) / canGrow.size());
            }
        }
    }

    @Override
    public void placeChildren() {
        growChildren();
        int yOffset = 0;
        for (LPGuiWidget child : getChildren()) {
            if (child instanceof WidgetContainer container) {
                container.setPos(0, yOffset + child.getMargin().getTop());
                container.placeChildren();
                yOffset += container.getHeight() + child.getMargin().getVertical() + getGap();
            } else {
                int childX = switch (child.getXPosition()) {
                    case LEFT -> 0;
                    case RIGHT -> getWidth() - child.getWidth();
                    case CENTER -> (getWidth() - child.getWidth()) / 2;
                };
                child.setPos(childX, yOffset + child.getMargin().getTop());
                yOffset += child.getHeight() + child.getMargin().getVertical() + getGap();
            }
        }
    }

    @Override
    public int getMinWidth() {
        int max = 0;
        for (LPGuiWidget child : getChildren()) {
            max = Math.max(max, child.getMinWidth());
        }
        return max;
    }

    @Override
    public int getMinHeight() {
        int sum = 0;
        for (LPGuiWidget child : getChildren()) {
            sum += child.getMinHeight() + child.getMargin().getVertical() + getGap();
        }
        return sum - getGap();
    }
}
