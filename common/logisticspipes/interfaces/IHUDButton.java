package logisticspipes.interfaces;

import net.minecraft.client.gui.GuiGraphics;

public interface IHUDButton {

	int getX();

	int getY();

	int sizeX();

	int sizeY();

	void setFocused();

	boolean isFocused();

	void clearFocused();

	void blockFocused();

	boolean isblockFocused();

	int focusedTime();

	void clicked();

	void renderButton(GuiGraphics guiGraphics, boolean hover, boolean clicked, boolean shifted);

	void renderAlways(GuiGraphics guiGraphics, boolean shifted);

	boolean shouldRenderButton();

	boolean buttonEnabled();
}
