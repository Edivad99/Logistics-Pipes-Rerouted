package logisticspipes.gui.hud;

import java.util.ArrayList;
import java.util.List;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HUDCrafting extends BasicHUDGui {

	private final PipeItemsCraftingLogistics pipe;

	public HUDCrafting(PipeItemsCraftingLogistics pipe) {
		this.pipe = pipe;
	}

	@Override
	public void renderHeadUpDisplay(double d, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
        GuiGraphics guiGraphics = SimpleGraphics.guiGraphics;
        if (!pipe.displayList.isEmpty()) {
			LPGuiGraphics.drawGuiBackGround(guiGraphics, -50, -28, 50, 30, 0, false);
		} else {
			LPGuiGraphics.drawGuiBackGround(guiGraphics, -30, -22, 30, 25, 0, false);
		}
		super.renderHeadUpDisplay(d, day, shifted, minecraft, config);
		int textColor = day ? 0xff404040 : 0xff7f7f7f;
		if (guiGraphics != null) {
			if (!pipe.displayList.isEmpty()) {
				guiGraphics.drawString(minecraft.font, "Result:", -20, -25, textColor, false);
				guiGraphics.drawString(minecraft.font, "Todo:", -20, 0, textColor, false);
			} else {
				guiGraphics.drawString(minecraft.font, "Result:", -25, -18, textColor, false);
			}
		}
		List<ItemIdentifierStack> list = new ArrayList<>();
		List<ItemIdentifierStack> craftables = pipe.getCraftedItems();
		if (craftables != null && !craftables.isEmpty()) {
			//TODO: handle multiple craftables.
			list.add(craftables.get(0));
		}
		if (!pipe.displayList.isEmpty()) {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, list, null, 0, 11, -18, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, pipe.displayList, null, 0, 13, 3, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, list, null, 0, -9, -1, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		return config.isHUDCrafting() && ((!pipe.hasCraftingSign() && pipe.getCraftedItems() != null) || pipe.displayList.size() > 0);
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		if (pipe.displayList.size() > 0) {
			return -50 < x && x < 50 && -28 < y && y < 30;
		} else {
			return -30 < x && x < 30 && -22 < y && y < 25;
		}
	}
}
