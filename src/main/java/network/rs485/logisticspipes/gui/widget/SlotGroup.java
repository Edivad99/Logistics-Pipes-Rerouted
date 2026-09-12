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
import logisticspipes.api.property.IBitSet;
import logisticspipes.api.util.IRectangle;
import logisticspipes.utils.gui.LPGuiGraphics;
import network.rs485.logisticspipes.gui.Drawable;
import network.rs485.logisticspipes.gui.GuiDrawer;
import network.rs485.logisticspipes.util.FuzzyFlag;
import network.rs485.logisticspipes.util.FuzzyUtil;

@Getter
public class SlotGroup extends LPGuiWidget {

    private static final int SLOT_SIZE = 18;

    private final List<Slot> slots;
    private final int columns;
    private final int rows;

    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;

    public SlotGroup(Drawable parent, HorizontalAlignment xPosition, VerticalAlignment yPosition, Margin margin,
        List<Slot> slots, int columns, int rows) {
        super(parent, xPosition, yPosition, Size.FIXED, Size.FIXED, margin);
        this.slots = slots;
        this.columns = columns;
        this.rows = rows;
        this.minWidth = columns * SLOT_SIZE;
        this.minHeight = rows * SLOT_SIZE;
        this.maxWidth = minWidth;
        this.maxHeight = minHeight;
    }

    @Override
    public void initWidget() {
        assert slots.size() == columns * rows;
        setSize(minWidth, minHeight);
    }

    @Override
    public void setPos(int x, int y) {
        super.setPos(x, y);
        int startX = getAbsoluteBody().getRoundedX() + 1;
        int startY = getAbsoluteBody().getRoundedY() + 1;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                SlotPositioning.setXY(slots.get(column + row * columns),
                    startX + column * SLOT_SIZE, startY + row * SLOT_SIZE);
            }
        }
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        super.draw(guiGraphics, mouseX, mouseY, delta, visibleArea);
        int startX = getAbsoluteBody().getRoundedX();
        int startY = getAbsoluteBody().getRoundedY();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = startX + column * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                LPGuiGraphics.drawSlotBackground(guiGraphics, x, y);
                if (slots.get(column + row * columns) instanceof FuzzyItemSlot fuzzySlot) {
                    drawFuzzyCorners(guiGraphics, fuzzySlot, x + 1, y + 1);
                }
            }
        }
    }

    /**
     * The corner marks telling which fuzzy flags a slot has set, one corner per flag, in the same
     * places and colours the older screens draw them.
     *
     * <p>Only the flags the slot actually offers get a corner: a slot that cannot use the ore
     * dictionary should not claim a corner for it.
     */
    private void drawFuzzyCorners(GuiGraphicsExtractor guiGraphics, FuzzyItemSlot slot, int x, int y) {
        IBitSet flags = slot.getFlagGetter().get();
        for (FuzzyFlag flag : slot.getUsedFlags()) {
            if (!FuzzyUtil.INSTANCE.get(flags, flag)) {
                continue;
            }
            int color = GuiDrawer.getFuzzyColor(flag);
            switch (flag) {
                case USE_ORE_DICT -> {
                    guiGraphics.fill(x + 8, y - 1, x + 17, y, color);
                    guiGraphics.fill(x + 16, y, x + 17, y + 8, color);
                }
                case IGNORE_DAMAGE -> {
                    guiGraphics.fill(x - 1, y - 1, x + 8, y, color);
                    guiGraphics.fill(x - 1, y, x, y + 8, color);
                }
                case IGNORE_NBT -> {
                    guiGraphics.fill(x - 1, y + 16, x + 8, y + 17, color);
                    guiGraphics.fill(x - 1, y + 8, x, y + 17, color);
                }
                case USE_ORE_CATEGORY -> {
                    guiGraphics.fill(x + 8, y + 16, x + 17, y + 17, color);
                    guiGraphics.fill(x + 16, y + 8, x + 17, y + 17, color);
                }
            }
        }
    }
}
