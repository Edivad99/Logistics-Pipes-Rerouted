
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public class ItemAmountSignCreationGui extends LogisticsBaseGuiScreen {

	public ItemAmountSignCreationGui(Player player, CoreRoutedPipe pipe, Direction dir) {
		super(buildDummy(player, pipe, dir), 180, 125, 0, 0);
	}
	private static DummyContainer buildDummy(Player player, CoreRoutedPipe pipe, Direction dir) {
		ItemAmountPipeSign sign = ((ItemAmountPipeSign) pipe.getPipeSign(dir));
		DummyContainer dummy = new DummyContainer(player.getInventory(), sign.itemTypeInv);
		dummy.addDummySlot(0, 10, 13);
		dummy.addNormalSlotsForPlayerInventory(11, 41);
		return dummy;
	}


	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 40);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9, topPos + 12);
	}
}
