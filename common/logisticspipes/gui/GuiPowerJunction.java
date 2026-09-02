
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.to_server.block.PowerJunctionCheatMessage;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiPowerJunction extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.powerjunction.";

	private final LogisticsPowerJunctionBlockEntity junction;

	public GuiPowerJunction(Player player, LogisticsPowerJunctionBlockEntity junction) {
		super(buildDummy(player, junction), 176, 166, 0, 0);
		this.junction = junction;
	}
	private static DummyContainer buildDummy(Player player, LogisticsPowerJunctionBlockEntity junction) {
		DummyContainer dummy = new DummyContainer(player, null, junction);
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}


	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiPowerJunction.PREFIX + "LogisticsPowerJunction"), 30, 8, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiPowerJunction.PREFIX + "StoredEnergy") + ":", 40, 23, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.formatNumberWithCommas(junction.getPowerLevel()) + " LP", 40, 33, 0xFF404040, false);
		guiGraphics.text(minecraft.font, "/ " + TextUtil.formatNumberWithCommas(LogisticsPowerJunctionBlockEntity.MAX_STORAGE) + " LP", 40, 43, 0xFF404040, false);
		guiGraphics.text(minecraft.font, "10 FE = 5 LP", 30, 58, 0xFF404040, false);
	}

	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/power_junction.png");

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiPowerJunction.TEXTURE, j, k, 0.0f, 0.0f, panelWidth, panelHeight, 256, 256);
		int level = 100 - junction.getChargeState();
		int levelPixels = level * 59 / 100;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiPowerJunction.TEXTURE, j + 10, k + 11 + levelPixels, 176.0f, levelPixels, 5, 59 - levelPixels, 256, 256);
	}

	@Override
	public void init() {
		super.init();
		if (LogisticsPipes.isDEBUG()) {
			logisticspipes.utils.gui.SmallGuiButton cheat = new logisticspipes.utils.gui.SmallGuiButton(0, leftPos + 140, topPos + 20, 20, 20, "+");
			cheat.setPressListener(b ->
					ClientPacketDistributor.sendToServer(new PowerJunctionCheatMessage(junction.getBlockPos())));
			addRenderableWidget(cheat);
		}
	}
}
