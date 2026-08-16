
package logisticspipes.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.LPConstants;
import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.string.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiPowerProvider extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.powerprovider.";

	private final LogisticsPowerProviderTileEntity junction;

	public GuiPowerProvider(Player player, LogisticsPowerProviderTileEntity junction) {
		super(buildDummy(player, junction), 176, 166, 0, 0);
		this.junction = junction;
	}
	private static DummyContainer buildDummy(Player player, LogisticsPowerProviderTileEntity junction) {
		DummyContainer dummy = new DummyContainer(player, null, junction);
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}


	private static final ResourceLocation TEXTURE = LPConstants.rl("textures/gui/power_junction.png");

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(GuiPowerProvider.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
		int level = 100 - junction.getChargeState();
		int levelPixels = level * 59 / 100;
		guiGraphics.blit(GuiPowerProvider.TEXTURE, j + 10, k + 11 + levelPixels, 176.0f, levelPixels, 5, 59 - levelPixels, 256, 256);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "Logistics" + junction.getBrand() + "PowerProvider"), 25, 8, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "StoredEnergy") + ":", 40, 25, 0x404040, false);
		guiGraphics.drawString(minecraft.font, StringUtils.getStringWithSpacesFromInteger(junction.getDisplayPowerLevel()) + " " + junction.getBrand(), 40, 35, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "/ " + StringUtils.getStringWithSpacesFromInteger(junction.getMaxStorage()) + " " + junction.getBrand(), 40, 45, 0x404040, false);
	}
}
