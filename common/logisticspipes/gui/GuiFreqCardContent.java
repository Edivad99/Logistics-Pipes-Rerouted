
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.world.inventory.FreqCardMenu;

public class GuiFreqCardContent extends LogisticsBaseGuiScreen {

	public GuiFreqCardContent(FreqCardMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 180, 130, 0, 0);
	}


	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 45);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 81, topPos + 14);
	}

}
