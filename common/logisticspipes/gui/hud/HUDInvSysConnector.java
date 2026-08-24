package logisticspipes.gui.hud;

import net.minecraft.client.Minecraft;

import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDInvSysConnector extends BasicHUDGui {

	private PipeItemsInvSysConnector pipe;
	private long display = System.currentTimeMillis();

	public HUDInvSysConnector(PipeItemsInvSysConnector pipe) {
		this.pipe = pipe;
	}

	@Override
	public void renderHeadUpDisplay(HUDDrawContext context, double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
        LPGuiGraphics.drawGuiBackGround(context, -50, -50, 50, 50, 0, false);
		super.renderHeadUpDisplay(context, distance, day, shifted, minecraft, config);
		int textColor = day ? 0xff404040 : 0xff7f7f7f;
		context.drawString(minecraft.font, "Expected:", -28, -25, textColor, false);
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, pipe.displayList, null, 0, -37, -18, 3, 9, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
	}

	@Override
	public boolean display(IHUDConfig config) {
		if (!config.isHUDInvSysCon()) {
			return false;
		}
		if (display > System.currentTimeMillis()) {
			return true;
		}
		if (!pipe.displayList.isEmpty()) {
			display = System.currentTimeMillis() + (2 * 1000);
		}
		return !pipe.displayList.isEmpty();
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return (-50 < x && x < 50 && -50 < y && y < 50);
	}
}
