/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import logisticspipes.utils.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

public class SmallGuiButton extends AbstractButton {

	/** Replaces the old Button.id field removed in 1.20.1 */
	public final int id;
	private final int stringOffset;
	private java.util.function.Consumer<SmallGuiButton> pressListener = b -> {};

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

	public void setPressListener(java.util.function.Consumer<SmallGuiButton> listener) {
		this.pressListener = listener;
	}

	@Override
	public void onPress() {
		pressListener.accept(this);
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Font fontrenderer = Minecraft.getInstance().font;
		boolean flag = isHovered();
		// Background fill
		int bg = !active ? 0xff444444 : flag ? 0xff888888 : 0xff666666;
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
		// Simple beveled border: lighter top/left, darker bottom/right
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xffaaaaaa);
		guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xff333333);
		guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, 0xffaaaaaa);
		guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xff333333);
		// Label
		int color = !active ? Color.getValue(Color.GREY) : flag ? Color.getValue(Color.LIGHT_YELLOW) : Color.getValue(Color.LIGHTER_GREY);
		guiGraphics.drawCenteredString(fontrenderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2 + stringOffset, color);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}
}
