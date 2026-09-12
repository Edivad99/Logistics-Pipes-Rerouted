/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
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

package network.rs485.logisticspipes.gui.widget;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import lombok.Getter;

import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.util.IRectangle;
import network.rs485.logisticspipes.gui.Drawable;
import network.rs485.logisticspipes.gui.GuiDrawer;
import network.rs485.logisticspipes.gui.MouseHoverable;
import network.rs485.logisticspipes.util.TextUtil;
import network.rs485.logisticspipes.util.math.MutableRectangle;

@Getter
public class LabelWidget extends LPGuiWidget implements MouseHoverable, Tooltipped {

    private final int fixedWidth;
    private final int textColor;
    private final HorizontalAlignment textAlignment;
    private final boolean extendable;
    private final int backgroundColor;

    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;

    private final MutableRectangle fullBody = new MutableRectangle();

    private String text;
    private String trimmedText;
    private boolean overflowing = false;

    public LabelWidget(Drawable parent, int width, HorizontalAlignment xPosition, VerticalAlignment yPosition,
        Size xSize, Margin margin, String text, int textColor, HorizontalAlignment textAlignment,
        boolean extendable, int backgroundColor) {
        super(parent, xPosition, yPosition, xSize, Size.FIXED, margin);
        this.fixedWidth = width;
        this.text = text;
        this.textColor = textColor;
        this.textAlignment = textAlignment;
        this.extendable = extendable;
        this.backgroundColor = backgroundColor;

        this.minWidth = switch (xSize) {
            case FIXED -> fixedWidth;
            case MIN -> textWidth(text) + 2;
            case GROW -> 30;
        };
        this.minHeight = GuiDrawer.getMcFontRenderer().lineHeight + 1;
        this.maxWidth = parent.getHeight();
        this.maxHeight = parent.getHeight();

        this.trimmedText = trimText(text);
    }

    @Override
    public void initWidget() {
        setSize(minWidth, minHeight);
        updateConstraints();
    }

    public void updateText(String newText) {
        text = newText;
        updateConstraints();
    }

    private void updateConstraints() {
        trimmedText = trimText(text);
        fullBody.setPosFromRectangle(getAbsoluteBody()).translate(0, -2).grow(1);
        fullBody.setSize(textWidth(text), minHeight);
        overflowing = !text.equals(trimmedText);
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        IRectangle body = getAbsoluteBody();
        if (backgroundColor != 0) {
            guiGraphics.fill(body.getRoundedLeft(), body.getRoundedTop(), body.getRoundedRight(),
                body.getRoundedBottom(), backgroundColor);
        }
        String drawText = extendable ? text : trimmedText;
        int drawnWidth = GuiDrawer.getMcFontRenderer().width(drawText);
        int textY = body.getRoundedTop()
            + (body.getRoundedHeight() - GuiDrawer.getMcFontRenderer().lineHeight) / 2;
        int textX = switch (textAlignment) {
            case LEFT -> body.getRoundedLeft() + 2;
            case CENTER -> body.getRoundedLeft() + (body.getRoundedWidth() - drawnWidth) / 2;
            case RIGHT -> body.getRoundedRight() - drawnWidth - 2;
        };
        guiGraphics.text(GuiDrawer.getMcFontRenderer(), drawText, textX, textY, textColor, false);
    }

    private String trimText(String text) {
        return TextUtil.getTrimmedString(text, getWidth(), GuiDrawer.getMcFontRenderer(), "...");
    }

    @Override
    public void setSize(int newWidth, int newHeight) {
        getRelativeBody().setSize(newWidth, newHeight);
        updateConstraints();
    }

    @Override
    public List<String> getTooltipText() {
        return overflowing && !extendable ? List.of(text) : List.of();
    }

    @Override
    public boolean isMouseHovering(float mouseX, float mouseY) {
        return getAbsoluteBody().contains(mouseX, mouseY);
    }

    private static int textWidth(String text) {
        return GuiDrawer.getMcFontRenderer().width(text);
    }

    @Override
    public String toString() {
        return "LabelWidget: " + text + ", " + getAbsoluteBody();
    }
}
