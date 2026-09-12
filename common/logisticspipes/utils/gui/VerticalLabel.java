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

package logisticspipes.utils.gui;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import network.rs485.logisticspipes.util.TextUtil;
import network.rs485.logisticspipes.util.math.MutableRectangle;

public class VerticalLabel extends Label {

    public VerticalLabel(String fullText, int x, int y, int maxLength, int textColor, int backgroundColor) {
        super(fullText, x, y, maxLength, textColor, backgroundColor);
    }

    @Override
    public boolean getOverflows() {
        return fullRect.getHeight() > maxLength;
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        hovered = hovered(mouseX, mouseY);
        MutableRectangle rect = hovered ? fullRect : trimmedRect;
        String text = hovered ? fullText : trimmedText;
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(rect.getX0(), rect.getY0() + rect.getHeight());
        // The 2D stack rotates about z by radians; the old Axis.ZP.rotationDegrees(-90f) quaternion
        // has no counterpart now that the pose is a Matrix3x2f.
        pose.rotate((float) -Math.PI / 2f);
        if (backgroundColor != 0) {
            guiGraphics.fill(-1, -1, fontRenderer.width(text) + 1, fontRenderer.lineHeight + 1, backgroundColor);
        }
        guiGraphics.text(fontRenderer, text, 0, 0, textColor, false);
        pose.popMatrix();
    }

    @Override
    public void setText(String newFullText) {
        fullText = newFullText;
        fullRect.setSize(fontRenderer.lineHeight, fontRenderer.width(fullText));

        trimmedText = TextUtil.getTrimmedString(fullText, maxLength, fontRenderer, "...");
        trimmedRect.setSize(fontRenderer.lineHeight, fontRenderer.width(trimmedText));

        int offset = (maxLength - trimmedRect.getRoundedHeight()) / 2;
        fullRect.setPos(x, y + offset);
        trimmedRect.setPos(x, y + offset);
    }
}
