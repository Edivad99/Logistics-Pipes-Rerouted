package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import logisticspipes.LPConstants;
import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.utils.string.StringUtils;
import logisticspipes.world.inventory.PowerProviderMenu;
import network.rs485.logisticspipes.util.TextUtil;

public class PowerProviderScreen extends LogisticsBaseGuiScreen<PowerProviderMenu> {

    private static final String PREFIX = "gui.powerprovider.";
    private static final Identifier TEXTURE = LPConstants.rl("textures/gui/power_junction.png");
    private final LogisticsPowerProviderTileEntity junction;

    public PowerProviderScreen(PowerProviderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, 0, 0);
        this.junction = menu.getBlockEntity();
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
        int j = leftPos;
        int k = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PowerProviderScreen.TEXTURE, j, k, 0.0f, 0.0f, panelWidth,
            panelHeight, 256, 256);
        int level = 100 - junction.getChargeState();
        int levelPixels = level * 59 / 100;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PowerProviderScreen.TEXTURE, j + 10, k + 11 + levelPixels,
            176.0f, levelPixels, 5, 59 - levelPixels, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font,
            TextUtil.translate(PowerProviderScreen.PREFIX + "Logistics" + junction.getBrand() + "PowerProvider"), 25, 8,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(PowerProviderScreen.PREFIX + "StoredEnergy") + ":", 40, 25,
            0xFF404040, false);
        guiGraphics.text(minecraft.font,
            StringUtils.getStringWithSpacesFromInteger(junction.getDisplayPowerLevel()) + " " + junction.getBrand(), 40,
            35, 0xFF404040, false);
        guiGraphics.text(minecraft.font,
            "/ " + StringUtils.getStringWithSpacesFromInteger(junction.getMaxStorage()) + " " + junction.getBrand(), 40,
            45, 0xFF404040, false);
    }
}
