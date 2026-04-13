
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;


import net.minecraft.world.entity.player.Player;

import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiFirewall extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.firewall.";

	private PipeItemsFirewall pipe;

	public GuiFirewall(PipeItemsFirewall pipe, Player player) {
		super(buildDummy(pipe, player), 230, 260, 0, 0);
		this.pipe = pipe;
	}
	private static DummyContainer buildDummy(PipeItemsFirewall pipe, Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.inv);
		dummy.addNormalSlotsForPlayerInventory(33, 175);
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				dummy.addDummySlot(x * 6 + y, x * 18 + 17, y * 18 + 41);
			}
		}
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		final String blocked = TextUtil.translate(GuiFirewall.PREFIX + "Blocked");
		final String allowed = TextUtil.translate(GuiFirewall.PREFIX + "Allowed");
		addRenderableWidget(new GuiStringHandlerButton(0, width / 2 + 23, height / 2 + 27 - 139, 60, 20, () -> pipe.isBlocking() ? blocked : allowed, () -> pipe.setBlocking(!pipe.isBlocking())));
		addRenderableWidget(new GuiStringHandlerButton(1, width / 2 + 23, height / 2 + 60 - 139, 60, 20, () -> pipe.isBlockProvider() ? blocked : allowed, () -> pipe.setBlockProvider(!pipe.isBlockProvider())));
		addRenderableWidget(new GuiStringHandlerButton(2, width / 2 + 23, height / 2 + 93 - 139, 60, 20, () -> pipe.isBlockCrafter() ? blocked : allowed, () -> pipe.setBlockCrafter(!pipe.isBlockCrafter())));
		addRenderableWidget(new GuiStringHandlerButton(3, width / 2 + 23, height / 2 + 126 - 139, 60, 20, () -> pipe.isBlockSorting() ? blocked : allowed, () -> pipe.setBlockSorting(!pipe.isBlockSorting())));
		addRenderableWidget(new GuiStringHandlerButton(4, width / 2 + 23, height / 2 + 160 - 139, 60, 20, () -> pipe.isBlockPower() ? blocked : allowed, () -> pipe.setBlockPower(!pipe.isBlockPower())));
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 33, topPos + 175);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Firewall"), leftPos + 45, topPos + 8, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Filter") + ":", leftPos + 14, topPos + 28, 0x404040);
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + x * 18 + 16, topPos + y * 18 + 40);
			}
		}
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Filtereditemsare") + ":", leftPos + 125, topPos + 8, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Providing") + ":", leftPos + 144, topPos + 41, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Crafting") + ":", leftPos + 146, topPos + 74, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Sorting") + ":", leftPos + 150, topPos + 107, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Powerflow") + ":", leftPos + 142, topPos + 141, 0x404040);
	}
}
