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

    /**
     * Reference equality, as in the original: the caller passes the very same instance back.
     */
    @SuppressWarnings("StringEquality")
    public boolean isTextEqual(String text) {
        return fullText == text;
    }

    protected boolean hovered(int mouseX, int mouseY) {
        return (hovered ? fullRect : trimmedRect).contains(mouseX, mouseY);
    }
}
