package logisticspipes.interfaces;

import net.minecraft.client.Minecraft;

import logisticspipes.renderer.HUDDrawContext;

public interface IHeadUpDisplayRenderer {

	/**
	 * @param guiGraphics the HUD panel draw target, with the billboard transform already applied to its
	 *                    pose. Passed down by the caller instead of being read from a global, so the HUD
	 *                    render path carries no dependency on ambient state.
	 */
	void renderHeadUpDisplay(HUDDrawContext context, double d, boolean day, boolean shifted, Minecraft mc, IHUDConfig config);

	boolean display(IHUDConfig config);

	boolean cursorOnWindow(int x, int y);

	void handleCursor(int x, int y);
}
