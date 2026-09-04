package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.to_server.block.PowerJunctionCheatMessage;
import logisticspipes.world.inventory.PowerJunctionMenu;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import network.rs485.logisticspipes.util.TextUtil;

public class PowerJunctionScreen extends LogisticsBaseGuiScreen<PowerJunctionMenu> {

    private static final String PREFIX = "gui.powerjunction.";
    private static final Identifier TEXTURE = LPConstants.rl("textures/gui/power_junction.png");
    private final LogisticsPowerJunctionBlockEntity junction;

    public PowerJunctionScreen(PowerJunctionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, 0, 0);
        this.junction = menu.getBlockEntity();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font, TextUtil.translate(PowerJunctionScreen.PREFIX + "LogisticsPowerJunction"), 30,
            8, 0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(PowerJunctionScreen.PREFIX + "StoredEnergy") + ":", 40, 23,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.formatNumberWithCommas(junction.getPowerLevel()) + " LP", 40, 33,
            0xFF404040, false);
        guiGraphics.text(minecraft.font,
            "/ " + TextUtil.formatNumberWithCommas(LogisticsPowerJunctionBlockEntity.MAX_STORAGE) + " LP", 40, 43,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, "10 FE = 5 LP", 30, 58, 0xFF404040, false);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
        int j = leftPos;
        int k = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PowerJunctionScreen.TEXTURE, j, k, 0.0f, 0.0f, panelWidth,
            panelHeight, 256, 256);
        int level = 100 - junction.getChargeState();
        int levelPixels = level * 59 / 100;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PowerJunctionScreen.TEXTURE, j + 10, k + 11 + levelPixels,
            176.0f, levelPixels, 5, 59 - levelPixels, 256, 256);
    }

    @Override
    public void init() {
        super.init();
        if (LogisticsPipes.isDEBUG()) {
            logisticspipes.utils.gui.SmallGuiButton cheat = new logisticspipes.utils.gui.SmallGuiButton(0,
                leftPos + 140, topPos + 20, 20, 20, "+");
            cheat.setPressListener(b ->
                ClientPacketDistributor.sendToServer(new PowerJunctionCheatMessage(junction.getBlockPos())));
            addRenderableWidget(cheat);
        }
    }
}
