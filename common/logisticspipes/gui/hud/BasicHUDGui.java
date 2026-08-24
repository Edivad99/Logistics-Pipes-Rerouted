package logisticspipes.gui.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.renderer.HUDDrawContext;

public abstract class BasicHUDGui implements IHeadUpDisplayRenderer {

	protected final List<IHUDButton> buttons = new ArrayList<>();

	protected void addRenderableWidget(IHUDButton button) {
		buttons.add(button);
	}

	@Override
	public void renderHeadUpDisplay(HUDDrawContext context, double d, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		for (IHUDButton button : buttons) {
			button.renderAlways(context, shifted);
			if (button.shouldRenderButton()) {
				button.renderButton(context, button.isFocused(), button.isblockFocused(), shifted);
			}
		}
	}

	@Override
	public void handleCursor(int x, int y) {
		for (IHUDButton button : buttons) {
			if (!button.buttonEnabled() || !button.shouldRenderButton()) {
				continue;
			}
			if ((button.getX() - 1 < x && x < (button.getX() + button.sizeX() + 1)) && (button.getY() - 1 < y && y < (button.getY() + button.sizeY() + 1))) {
				if (!button.isFocused() && !button.isblockFocused()) {
					button.setFocused();
				} else if (button.focusedTime() > 400 && !button.isblockFocused()) {
					button.clicked();
					button.blockFocused();
				}
			} else if (button.isFocused() || button.isblockFocused()) {
				button.clearFocused();
			}
		}
	}
}
