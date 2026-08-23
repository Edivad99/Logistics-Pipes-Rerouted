package logisticspipes.interfaces;

import logisticspipes.renderer.HUDDrawContext;

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

	void renderButton(HUDDrawContext context, boolean hover, boolean clicked, boolean shifted);

	void renderAlways(HUDDrawContext context, boolean shifted);

	boolean shouldRenderButton();

	boolean buttonEnabled();
}
