/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import java.util.function.Consumer;

import logisticspipes.utils.Color;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import lombok.Setter;

public class SmallGuiButton extends AbstractButton {

	/** Replaces the old Button.id field removed in 1.20.1 */
	public final int id;
	private final int stringOffset;
    @Setter
	private Consumer<SmallGuiButton> pressListener = b -> {};

	public SmallGuiButton(int buttonId, int x, int y, int width, int height, String label) {
		this(buttonId, x, y, width, height, label, 0);
	}

	public SmallGuiButton(int buttonId, int x, int y, int width, int height, String label, int offset) {
		super(x, y, width, height, Component.literal(label));
		this.id = buttonId;
		this.stringOffset = offset;
	}

	public SmallGuiButton(int buttonId, int x, int y, String label) {
		super(x, y, 20, 20, Component.literal(label));
		this.id = buttonId;
		this.stringOffset = 0;
	}

    @Override
	public void onPress() {
		pressListener.accept(this);
	}

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        int minX = this.getX() + 2;
        int maxX = this.getX() + this.getWidth() - 2;
        renderScrollingString(guiGraphics, font, this.getMessage(), minX, this.getY() + stringOffset, maxX, this.getY() + this.getHeight() + stringOffset, color);
    }

    @Override
    public int getFGColor() {
        if (!active) {
            return Color.getValue(Color.GREY);
        }
        return isHovered() ? Color.getValue(Color.LIGHT_YELLOW) : Color.getValue(Color.LIGHTER_GREY);
    }

    @Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}
}
