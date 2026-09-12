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
import java.util.function.IntPredicate;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.util.IRectangle;
import logisticspipes.utils.Color;
import network.rs485.logisticspipes.gui.Drawable;
import network.rs485.logisticspipes.gui.GuiDrawer;
import network.rs485.logisticspipes.util.TextUtil;

public class TextButton extends LPGuiButton implements Tooltipped {

    private String text;
    private String trimmedText;

    public TextButton(Drawable parent, HorizontalAlignment xPosition, VerticalAlignment yPosition, Size xSize,
        Size ySize, Margin margin, String text, boolean enabled, IntPredicate onClickAction) {
        super(parent, xPosition, yPosition, xSize, ySize, margin, onClickAction);
        setEnabled(enabled);
        this.text = text;
        this.trimmedText = trimText(text);
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = value;
        trimmedText = trimText(value);
    }

    // Computed on demand, not stored: at construction time the widget has not been laid out yet, so
    // the height is still 0 and a stored offset would come out negative -- drawing the label above
    // the button instead of centred in it.
    public int getYOffset() {
        return ((getRelativeBody().getRoundedHeight() - GuiDrawer.getMcFontRenderer().lineHeight) / 2) + 1;
    }

    @Override
    public void setSize(int newWidth, int newHeight) {
        super.setSize(newWidth, newHeight);
        setText(text);
    }

    private String trimText(String text) {
        return TextUtil.getTrimmedString(text, getRelativeBody().getRoundedWidth() - 4, GuiDrawer.getMcFontRenderer(), "...");
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        super.draw(guiGraphics, mouseX, mouseY, delta, visibleArea);
        if (!isVisible()) {
            return;
        }
        int color = isEnabled() ? Color.WHITE.getValue() : 0xFFA0A0A0;
        int textWidth = GuiDrawer.getMcFontRenderer().width(trimmedText);
        int cx = getAbsoluteBody().getRoundedLeft() + getAbsoluteBody().getRoundedWidth() / 2 - textWidth / 2;
        int cy = getAbsoluteBody().getRoundedTop() + getYOffset();
        guiGraphics.text(GuiDrawer.getMcFontRenderer(), trimmedText, cx, cy, color, true);
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int mouseButton) {
        return isEnabled() && getOnClickAction().test(mouseButton);
    }

    @Override
    public List<String> getTooltipText() {
        return trimmedText.equals(text) ? List.of() : List.of(text);
    }
}
