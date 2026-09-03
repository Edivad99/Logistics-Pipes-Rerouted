
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.world.inventory.ItemAmountSignMenu;

public class ItemAmountSignCreationGui extends LogisticsBaseGuiScreen {

	public ItemAmountSignCreationGui(ItemAmountSignMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 180, 125, 0, 0);
	}


	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 40);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9, topPos + 12);
	}
}
