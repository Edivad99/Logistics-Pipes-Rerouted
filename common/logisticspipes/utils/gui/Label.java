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

package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import logisticspipes.utils.TextUtil;
import logisticspipes.utils.math.MutableRectangle;

public class Label {

    protected final int x;
    protected final int y;
    protected final int maxLength;
    protected final int textColor;
    protected final int backgroundColor;

    protected final Font fontRenderer = Minecraft.getInstance().font;

    protected final MutableRectangle fullRect = new MutableRectangle();
    protected final MutableRectangle trimmedRect = new MutableRectangle();

    protected String fullText = "";
    protected String trimmedText = "";
    protected boolean hovered = false;

    public Label(String fullText, int x, int y, int maxLength, int textColor, int backgroundColor) {
        this.x = x;
        this.y = y;
        this.maxLength = maxLength;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        fullRect.setPos(x, y);
        trimmedRect.setPos(x, y);
        setText(fullText);
    }

    public boolean getOverflows() {
        return fullRect.getWidth() > maxLength;
    }

    public void draw(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        hovered = hovered(mouseX, mouseY);
        MutableRectangle rect = hovered ? fullRect : trimmedRect;
        String text = hovered ? fullText : trimmedText;
        if (backgroundColor != 0) {
            guiGraphics.fill(rect.getRoundedLeft() - 1, rect.getRoundedTop() - 1,
                rect.getRoundedRight() + 1, rect.getRoundedBottom() + 1, backgroundColor);
        }
        guiGraphics.text(fontRenderer, text, rect.getRoundedLeft(), rect.getRoundedTop(), textColor, false);
    }

    public void setText(String newFullText) {
        fullText = newFullText;
        fullRect.setSize(fontRenderer.width(fullText), fontRenderer.lineHeight);

        trimmedText = TextUtil.getTrimmedString(fullText, maxLength, fontRenderer, "...");
        trimmedRect.setSize(fontRenderer.width(trimmedText), fontRenderer.lineHeight);

        int offset = (maxLength - trimmedRect.getRoundedWidth()) / 2;
        fullRect.setPos(x + offset, y);
        trimmedRect.setPos(x + offset, y);
    }

    /** Reference equality, as in the original: the caller passes the very same instance back. */
    @SuppressWarnings("StringEquality")
    public boolean isTextEqual(String text) {
        return fullText == text;
    }

    protected boolean hovered(int mouseX, int mouseY) {
        return (hovered ? fullRect : trimmedRect).contains(mouseX, mouseY);
    }
}
