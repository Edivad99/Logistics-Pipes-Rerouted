package logisticspipes.gui;

import logisticspipes.world.item.LPItems;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.world.item.ItemModule;
import logisticspipes.utils.CardManagementInventory;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class GuiCardManager extends LogisticsBaseGuiScreen {

	public GuiCardManager(Player player) {
		super(buildDummy(player), 180, 180, 0, 0);
	}

	private static DummyContainer buildDummy(Player player) {
		final CardManagementInventory Cinv = new CardManagementInventory();
		DummyContainer dummy = new DummyContainer(player, Cinv, new IGuiOpenControler() {

			@Override
			public void guiOpenedByPlayer(Player player) {}

			@Override
			public void guiClosedByPlayer(Player player) {
				Cinv.close(player, (int) player.getX(), (int) player.getY(), (int) player.getZ());
			}
		});
		dummy.addRestrictedSlot(0, Cinv, 21, 21, ItemModule.class);
		dummy.addRestrictedSlot(1, Cinv, 61, 21, ItemModule.class);
		dummy.addRestrictedSlot(2, Cinv, 41, 58, itemStack -> false);
		dummy.addRestrictedSlot(3, Cinv, 121, 39, LPItems.ITEM_CARD.get());
		for (int i = 4; i < 7; i++) {
			dummy.addColorSlot(i, Cinv, 101, 21 + (i - 4) * 18);
		}
		for (int i = 7; i < 10; i++) {
			dummy.addColorSlot(i, Cinv, 141, 21 + (i - 7) * 18);
		}
		dummy.addNormalSlotsForPlayerInventory(10, 95);
		return dummy;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int j, int k) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, bottom - 85);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 20, topPos + 20);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 60, topPos + 20);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 40, topPos + 57);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 120, topPos + 38);
		guiGraphics.fill(leftPos + 38, topPos + 27, leftPos + 60, topPos + 31, Color.getValue(Color.DARKER_GREY));
		guiGraphics.fill(leftPos + 47, topPos + 31, leftPos + 51, topPos + 57, Color.getValue(Color.DARKER_GREY));
		for (int i = 0; i < 3; i++) {
			LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 100, topPos + 20 + i * 18);
		}
		for (int i = 0; i < 3; i++) {
			LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 140, topPos + 20 + i * 18);
		}
	}
}
