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
import net.minecraft.world.inventory.Slot;

import lombok.Getter;

import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.util.IRectangle;
import logisticspipes.utils.gui.LPGuiGraphics;
import network.rs485.logisticspipes.gui.Drawable;

@Getter
public class PlayerInventorySlotGroup extends LPGuiWidget {

    private static final int SLOT_SIZE = 18;

    private final List<Slot> slots;

    private final int minWidth = 9 * SLOT_SIZE;
    private final int minHeight = 4 * SLOT_SIZE + 4;
    private final int maxWidth = minWidth;
    private final int maxHeight = minHeight;

    public PlayerInventorySlotGroup(Drawable parent, HorizontalAlignment xPosition, VerticalAlignment yPosition,
        Margin margin, List<Slot> slots) {
        super(parent, xPosition, yPosition, Size.FIXED, Size.FIXED, margin);
        this.slots = slots;
    }

    @Override
    public void initWidget() {
        assert slots.size() == 4 * 9;
        setSize(minWidth, minHeight);
    }

    @Override
    public void setPos(int x, int y) {
        super.setPos(x, y);
        int startX = getAbsoluteBody().getRoundedX() + 1;
        int startY = getAbsoluteBody().getRoundedY() + 1;
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                SlotPositioning.setXY(slots.get(index), startX + column * SLOT_SIZE, startY + row * SLOT_SIZE);
                index++;
            }
        }

        // Add the hotbar inventory slots
        for (int column = 0; column < 9; column++) {
            SlotPositioning.setXY(slots.get(index), startX + column * SLOT_SIZE, startY + 3 * SLOT_SIZE + 4);
            index++;
        }
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        super.draw(guiGraphics, mouseX, mouseY, delta, visibleArea);
        int startX = getAbsoluteBody().getRoundedX();
        int startY = getAbsoluteBody().getRoundedY();
        // 3 × 9 backpack
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                LPGuiGraphics.drawSlotBackground(guiGraphics, startX + column * SLOT_SIZE, startY + row * SLOT_SIZE);
            }
        }
        // Hotbar (4px gap)
        for (int column = 0; column < 9; column++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, startX + column * SLOT_SIZE, startY + 3 * SLOT_SIZE + 4);
        }
    }
}
