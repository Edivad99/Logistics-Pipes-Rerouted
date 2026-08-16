
package logisticspipes.gui;

import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.TextUtil;

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
	protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 33, topPos + 175);
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + x * 18 + 16, topPos + y * 18 + 40);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Firewall"), 45, 8, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Filter") + ":", 14, 28, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Filtereditemsare") + ":", 125, 8, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Providing") + ":", 144, 41, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Crafting") + ":", 146, 74, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Sorting") + ":", 150, 107, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFirewall.PREFIX + "Powerflow") + ":", 142, 141, 0x404040, false);
	}
}
