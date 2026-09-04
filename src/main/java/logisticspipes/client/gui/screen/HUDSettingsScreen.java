package logisticspipes.client.gui.screen;

import java.io.IOException;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.SneakyThrows;

import logisticspipes.hud.HUDConfig;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.network.to_server.config.SetHudSettingMessage;
import logisticspipes.network.to_server.config.SetHudSettingMessage.HudSetting;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.world.inventory.HudSettingsMenu;
import logisticspipes.world.item.LPItems;

public class HUDSettingsScreen extends LogisticsBaseGuiScreen<HudSettingsMenu> {

    private final int slot;
    private final Player player;

    public HUDSettingsScreen(HudSettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 180, 160, 0, 0);
        this.slot = menu.getSlot();
        this.player = inventory.player;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void init() {
        super.init();
        if (!player.getInventory().getItem(slot).isEmpty()) {
            IHUDConfig config = new HUDConfig(player.getInventory().getItem(slot));
            addRenderableWidget(wire(new GuiCheckBox(0, leftPos + 30, topPos + 10, 12, 12, config.isChassisHUD())));
            addRenderableWidget(wire(new GuiCheckBox(1, leftPos + 30, topPos + 30, 12, 12, config.isHUDCrafting())));
            addRenderableWidget(wire(new GuiCheckBox(2, leftPos + 30, topPos + 50, 12, 12, config.isHUDInvSysCon())));
            addRenderableWidget(wire(new GuiCheckBox(3, leftPos + 30, topPos + 70, 12, 12, config.isHUDPowerLevel())));
            addRenderableWidget(wire(new GuiCheckBox(4, leftPos + 30, topPos + 90, 12, 12, config.isHUDProvider())));
            addRenderableWidget(wire(new GuiCheckBox(5, leftPos + 30, topPos + 110, 12, 12, config.isHUDSatellite())));
        } else {
            closeGui();
        }
    }

    private GuiCheckBox wire(GuiCheckBox cb) {
        cb.setPressListener(b -> ClientPacketDistributor.sendToServer(
            new SetHudSettingMessage(slot, HudSetting.values()[b.id], b.getState())));
        return cb;
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
        if (player.getInventory().getItem(slot).isEmpty()
            || player.getInventory().getItem(slot).getItem() != LPItems.HUD_GLASSES.get()) {
            minecraft.player.closeContainer();
        }
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        LPGuiGraphics.drawPlayerHotbarBackground(guiGraphics, leftPos + 10, topPos + 134);
        LPGuiGraphics.drawPlayerArmorBackground(guiGraphics, leftPos + 10, topPos + 65);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font, "HUD Chassis Pipe", 50, 13, 0xFF4c4c4c, false);
        guiGraphics.text(minecraft.font, "HUD Crafting Pipe", 50, 33, 0xFF4c4c4c, false);
        guiGraphics.text(minecraft.font, "HUD InvSysCon Pipe", 50, 53, 0xFF4c4c4c, false);
        guiGraphics.text(minecraft.font, "HUD Power Junction", 50, 73, 0xFF4c4c4c, false);
        guiGraphics.text(minecraft.font, "HUD Provider Pipe", 50, 93, 0xFF4c4c4c, false);
        guiGraphics.text(minecraft.font, "HUD Satellite Pipe", 50, 113, 0xFF4c4c4c, false);
    }
}
