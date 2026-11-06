package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiStringHandlerButton extends Button {

    private final StringHandler handler;

    public GuiStringHandlerButton(int x, int y, int width, int height, StringHandler handler, Runnable onPress) {
        super(x, y, width, height, handler.getContent(), b -> onPress.run(), DEFAULT_NARRATION);
        this.handler = handler;
    }

    /**
     * 1.21.11 made {@code Button} abstract and {@code renderWidget} final: what a subclass fills in
     * is {@code renderContents}. Drawing matches {@code Button.Plain}, the concrete vanilla
     * variant; the point of this class is the message refresh in front of it.
     */
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        setMessage(handler.getContent());
        extractDefaultSprite(guiGraphics);
        extractDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    public interface StringHandler {

        Component getContent();
    }

}
