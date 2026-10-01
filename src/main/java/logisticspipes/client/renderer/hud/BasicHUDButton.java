package logisticspipes.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import logisticspipes.client.renderer.hud.HUDDrawContext;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.utils.Color;

public abstract class BasicHUDButton implements IHUDButton {

    // Panel content is nearly coplanar once projected into the world, so anything drawn at the panel's own
    // z compares almost-equal against it and comes out stippled. Backgrounds dodge this by not writing depth
    // (see LPGuiGraphics#drawGuiBackGround), but fill() goes through RenderType.GUI, which sets its own
    // COLOR_DEPTH_WRITE mask when the batch is drawn and so ignores RenderSystem.depthMask(). Buttons
    // therefore step toward the viewer instead, the way GuiGraphicsExtractor does for items (+150) and their count
    // labels (+200). Both steps stay well under 150 so a button never covers an item.
    public static final int BUTTON_Z = 30;
    public static final int BUTTON_LABEL_Z = 15;

    protected final int posX;
    protected final int posY;
    protected final int sizeX;
    protected final int sizeY;
    protected long focusedTimeStart = 0;
    protected final Component label;

    public BasicHUDButton(String name, int x, int y, int width, int height) {
        this(Component.literal(name), x, y, width, height);
    }

    public BasicHUDButton(Component name, int x, int y, int width, int height) {
        label = name;
        posX = x;
        posY = y;
        sizeX = width;
        sizeY = height;
    }

    @Override
    public int getX() {
        return posX;
    }

    @Override
    public int getY() {
        return posY;
    }

    @Override
    public int sizeX() {
        return sizeX;
    }

    @Override
    public int sizeY() {
        return sizeY;
    }

    @Override
    public void blockFocused() {
        focusedTimeStart = -1;
    }

    @Override
    public boolean isblockFocused() {
        return focusedTimeStart == -1;
    }

    @Override
    public void setFocused() {
        focusedTimeStart = System.currentTimeMillis();
    }

    @Override
    public boolean isFocused() {
        return focusedTimeStart != 0;
    }

    @Override
    public void clearFocused() {
        focusedTimeStart = 0;
    }

    @Override
    public int focusedTime() {
        return (int) (System.currentTimeMillis() - focusedTimeStart);
    }

    @Override
    public void renderButton(HUDDrawContext gg, boolean hover, boolean clicked, boolean shifted) {
        int bg = clicked ? 0xaa333333 : hover ? 0xaa555555 : 0xaa444444;
        gg.fill(posX, posY, posX + sizeX, posY + sizeY, bg);
        gg.fill(posX, posY, posX + sizeX, posY + 1, 0xffaaaaaa);
        gg.fill(posX, posY + sizeY - 1, posX + sizeX, posY + sizeY, 0xff333333);
        gg.drawCenteredString(Minecraft.getInstance().font, label.getString(),
            posX + sizeX / 2, posY + (sizeY - 8) / 2, Color.getValue(Color.LIGHTER_GREY), true);
    }

    @Override
    public void renderAlways(HUDDrawContext guiGraphics, boolean shifted) {

    }
}
