package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.joml.Matrix3x2fStack;

import logisticspipes.utils.TextUtil;
import logisticspipes.utils.math.MutableRectangle;

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
