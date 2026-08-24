/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import java.util.function.Consumer;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import lombok.Setter;

import logisticspipes.utils.Color;

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
	public void onPress(InputWithModifiers input) {
		pressListener.accept(this);
	}

    /**
     * 1.21.11 made {@code renderWidget} final and split it: subclasses fill in
     * {@code renderContents}, and text is requested from an {@link ActiveTextCollector} rather than
     * drawn onto the {@link GuiGraphicsExtractor}.
     *
     * <p>This does not call {@code renderDefaultLabel} because LP offsets the label vertically by
     * {@code stringOffset}, which is the whole reason this class exists; the rest is that method's
     * body. Two things worth knowing about the collector: it takes a vertical <em>band</em> rather
     * than a baseline, and it has no colour argument at all -- the colour rides on the component's
     * style, which is how {@code AbstractButton} applies {@code getFGColor} too.</p>
     */
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(guiGraphics);
        Component message = getMessage().copy().withStyle(style -> style.withColor(getFGColor()));
        guiGraphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
            .acceptScrollingWithDefaultCenter(
                message,
                this.getX() + TEXT_MARGIN,
                this.getX() + this.getWidth() - TEXT_MARGIN,
                this.getY() + stringOffset,
                this.getY() + this.getHeight() + stringOffset);
    }

    @Override
    public int getFGColor() {
        if (!active) {
            return Color.getValue(Color.GREY);
        }
        return isHovered() ? Color.getValue(Color.LIGHT_YELLOW) : Color.getValue(Color.LIGHTER_GREY);
    }

    @Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}
}
