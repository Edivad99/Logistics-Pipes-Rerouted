package logisticspipes.client.gui.screen;

import java.io.IOException;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.SneakyThrows;

import logisticspipes.network.to_server.config.SetHudSettingMessage;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.world.inventory.HudSettingsMenu;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.HUDComponent;
import logisticspipes.world.item.component.HudSetting;
import logisticspipes.world.item.component.LPDataComponents;

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
            final HUDComponent config = player.getInventory().getItem(slot)
                .getOrDefault(LPDataComponents.HUD, HUDComponent.DEFAULT);
            for (HudSetting setting : HudSetting.values()) {
                addRenderableWidget(wire(new GuiCheckBox(setting.ordinal(), leftPos + 30,
                    topPos + rowY(setting), 12, 12, config.get(setting))));
            }
        } else {
            closeGui();
        }
    }

    private GuiCheckBox wire(GuiCheckBox cb) {
        cb.setPressListener(b -> ClientPacketDistributor.sendToServer(
            new SetHudSettingMessage(slot, HudSetting.values()[b.id], b.change())));
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
        for (HudSetting setting : HudSetting.values()) {
            guiGraphics.text(minecraft.font, label(setting), 50, rowY(setting) + 3, 0xFF4c4c4c, false);
        }
    }

    private static int rowY(HudSetting setting) {
        return 10 + 20 * setting.ordinal();
    }

    private static String label(HudSetting setting) {
        return switch (setting) {
            case CHASSIS -> "HUD Chassis Pipe";
            case CRAFTING -> "HUD Crafting Pipe";
            case INV_SYS_CON -> "HUD InvSysCon Pipe";
            case POWER_JUNCTION -> "HUD Power Junction";
            case PROVIDER -> "HUD Provider Pipe";
            case SATELLITE -> "HUD Satellite Pipe";
        };
    }
}
