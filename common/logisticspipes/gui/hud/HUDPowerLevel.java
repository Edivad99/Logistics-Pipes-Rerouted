package logisticspipes.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.gui.LPGuiGraphics;

public class HUDPowerLevel extends BasicHUDGui implements IHeadUpDisplayRenderer {

	private final IPowerLevelDisplay junction;
	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/power_junction.png");

	public HUDPowerLevel(IPowerLevelDisplay junction) {
		this.junction = junction;
	}

	@Override
	public void renderHeadUpDisplay(HUDDrawContext context, double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
        LPGuiGraphics.drawGuiBackGround(context, -60, -40, 60, 40, 0, false);
		super.renderHeadUpDisplay(context, distance, day, shifted, minecraft, config);
		// blit() draws immediately and would write depth over the panel it sits on, which stipples the bar
		// against the coplanar background. Layer it by draw order instead -- see LPGuiGraphics#drawGuiBackGround.
		try {
			// Frame (uv 9,10 size 7x61 on 256x256 texture)
			context.blit(TEXTURE, -50, -30, 9.0f, 10.0f, 7, 61, 256, 256);
			int level = 100 - junction.getChargeState();
			int filled = 59 - (level * 59 / 100);
			if (filled > 0) {
				// Fill bar (uv 176, level*59/100 size 5 x filled)
				context.blit(TEXTURE, -49, -29 + (level * 59 / 100), 176.0f, (float)(level * 59 / 100), 5, filled, 256, 256);
			}
		} finally {
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		return !junction.isHUDInvalid() && config.isHUDPowerLevel();
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -60 < x && x < 60 && -40 < y && y < 40;
	}

}
