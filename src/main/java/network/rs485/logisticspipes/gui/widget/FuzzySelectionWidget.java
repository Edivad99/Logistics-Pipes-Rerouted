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

package network.rs485.logisticspipes.gui.widget;

import java.util.BitSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import lombok.Getter;

import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.Margin;
import logisticspipes.api.gui.Size;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.property.BitSetProperty;
import logisticspipes.api.property.IBitSet;
import logisticspipes.api.property.layer.PropertyOverlay;
import logisticspipes.api.util.IRectangle;
import logisticspipes.utils.Color;
import network.rs485.logisticspipes.gui.BaseGuiContainer;
import network.rs485.logisticspipes.gui.Drawable;
import network.rs485.logisticspipes.gui.GuiDrawer;
import network.rs485.logisticspipes.gui.MouseInteractable;
import network.rs485.logisticspipes.util.FuzzyFlag;
import network.rs485.logisticspipes.util.FuzzyUtil;
import network.rs485.logisticspipes.util.TextUtil;

public class FuzzySelectionWidget extends LPGuiWidget implements MouseInteractable {

    private static final String FLAG_PREFIX = "enum.logisticspipes.fuzzy_flags.";
    private static final int BORDER = 5;

    private final PropertyOverlay<BitSet, BitSetProperty> fuzzyFlagOverlay;

    @Getter
    private boolean active = false;

    @Getter
    private @Nullable FuzzyItemSlot currentSlot = null;

    public FuzzySelectionWidget(Drawable parent, PropertyOverlay<BitSet, BitSetProperty> fuzzyFlagOverlay) {
        super(parent, HorizontalAlignment.LEFT, VerticalAlignment.TOP, Size.FIXED, Size.FIXED, Margin.NONE);
        this.fuzzyFlagOverlay = fuzzyFlagOverlay;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCurrentSlot(@Nullable FuzzyItemSlot newSlot) {
        currentSlot = newSlot;
        if (newSlot != null) {
            setSize(calculateWidth(newSlot.getUsedFlags()), calculateHeight(newSlot.getUsedFlags()));
        }
    }

    private Set<FuzzyFlag> currentFlags() {
        FuzzyItemSlot slot = currentSlot;
        return slot == null ? Set.of() : slot.getUsedFlags();
    }

    @Override
    public int getMinWidth() {
        return calculateWidth(currentFlags());
    }

    @Override
    public int getMinHeight() {
        return calculateHeight(currentFlags());
    }

    @Override
    public int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public int getMaxHeight() {
        return getMinHeight();
    }

    @Override
    public void initWidget() {
    }

    private int calculateWidth(Set<FuzzyFlag> flags) {
        int max = 0;
        for (FuzzyFlag flag : flags) {
            max = Math.max(max, Minecraft.getInstance().font.width(label(flag) + BORDER * 2));
        }
        return max;
    }

    private int calculateHeight(Set<FuzzyFlag> flags) {
        return flags.size() * 10 + BORDER * 2;
    }

    private static String label(FuzzyFlag flag) {
        return TextUtil.translate(FLAG_PREFIX + flag.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, float mouseX, float mouseY, float delta,
        IRectangle visibleArea) {
        FuzzyItemSlot slot = currentSlot;
        if (!active || slot == null) {
            return;
        }
        boolean hoveringSlot = Minecraft.getInstance().screen instanceof BaseGuiContainer<?> container
            && container.getCurrentHoveredSlot() == slot;
        if (!isMouseHovering(mouseX, mouseY) && !hoveringSlot) {
            return;
        }
        GuiDrawer.drawGuiBackground(guiGraphics, getRelativeBody());
        int yOffset = BORDER;
        IBitSet flags = slot.getFlagGetter().get();
        for (FuzzyFlag flag : slot.getUsedFlags()) {
            int color = FuzzyUtil.INSTANCE.get(flags, flag) ? GuiDrawer.getFuzzyColor(flag) : Color.TEXT_DARK.getValue();
            guiGraphics.text(GuiDrawer.getMcFontRenderer(), label(flag),
                getRelativeBody().getRoundedLeft() + BORDER, getRelativeBody().getRoundedTop() + yOffset,
                color, false);
            yOffset += 10;
        }
    }

    @Override
    public boolean isMouseHovering(float mouseX, float mouseY) {
        return getRelativeBody().copy().grow(2).translate(-1, -1).contains(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int mouseButton) {
        FuzzyItemSlot slot = currentSlot;
        if (slot == null) {
            return false;
        }
        FuzzyFlag clickedFlag = getHoveredFlag(mouseX, mouseY);
        if (clickedFlag == null) {
            return false;
        }
        fuzzyFlagOverlay.writeVoid(p -> p.flip(slot.getSlotIndex() * 4 + clickedFlag.getBit()));
        return true;
    }

    private @Nullable FuzzyFlag getHoveredFlag(float mouseX, float mouseY) {
        FuzzyItemSlot slot = currentSlot;
        if (slot == null) {
            return null;
        }
        for (FuzzyFlag flag : slot.getUsedFlags()) {
            boolean hit = getRelativeBody().copy()
                .translate(BORDER, BORDER + 10 * flag.ordinal())
                .setSize(getRelativeBody().getRoundedWidth(), 10)
                .contains(mouseX, mouseY);
            if (hit) {
                return flag;
            }
        }
        return null;
    }
}
