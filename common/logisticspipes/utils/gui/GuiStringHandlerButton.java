package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiStringHandlerButton extends Button {

	private final StringHandler handler;

	// par1 = legacy id (discarded), par2 = x, par3 = y
	public GuiStringHandlerButton(int par1, int par2, int par3, StringHandler handler) {
		super(par2, par3, 200, 20, Component.literal(handler.getContent()), b -> {}, DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, with onPress callback
	public GuiStringHandlerButton(int par1, int par2, int par3, StringHandler handler, Runnable onPress) {
		super(par2, par3, 200, 20, Component.literal(handler.getContent()), b -> onPress.run(), DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, par4 = width, par5 = height
	public GuiStringHandlerButton(int par1, int par2, int par3, int par4, int par5, StringHandler handler) {
		super(par2, par3, par4, par5, Component.literal(handler.getContent()), b -> {}, DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, par4 = width, par5 = height, with onPress callback
	public GuiStringHandlerButton(int par1, int par2, int par3, int par4, int par5, StringHandler handler, Runnable onPress) {
		super(par2, par3, par4, par5, Component.literal(handler.getContent()), b -> onPress.run(), DEFAULT_NARRATION);
		this.handler = handler;
	}

	/**
	 * 1.21.11 made {@code Button} abstract and {@code renderWidget} final: what a subclass fills in
	 * is {@code renderContents}. Drawing matches {@code Button.Plain}, the concrete vanilla
	 * variant; the point of this class is the message refresh in front of it.
	 */
	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		setMessage(Component.literal(handler.getContent()));
		renderDefaultSprite(guiGraphics);
		renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
	}

	public interface StringHandler {

		String getContent();
	}

}
