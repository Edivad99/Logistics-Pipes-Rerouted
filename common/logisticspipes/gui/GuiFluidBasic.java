
package logisticspipes.gui;

import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import network.rs485.logisticspipes.util.TextUtil;

public class GuiFluidBasic extends LogisticsBaseGuiScreen {

	public GuiFluidBasic(Player player, Container inventory) {
		super(buildDummy(player, inventory), 180, 130, 0, 0);
	}
	private static DummyContainer buildDummy(Player player, Container inventory) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), inventory);
		dummy.addFluidSlot(0, inventory, 28, 13);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}


	@Override
	public void init() {
		super.init();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 45);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 27, topPos + 12);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		if (getMenu().getSlot(0).getItem().is(Items.AIR)) {
			guiGraphics.drawString(minecraft.font, TextUtil.translate("gui.fluidbasic.Empty"), 50, 18, 0x404040, false);
		} else {
			guiGraphics.drawString(minecraft.font, ItemIdentifier.get(getMenu().getSlot(0).getItem()).getFriendlyName(), 50, 18, 0x404040, false);
		}
	}
}
