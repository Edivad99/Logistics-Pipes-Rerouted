package logisticspipes.client.gui.popup;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.Translations;
import logisticspipes.network.to_server.security.SaveSecuritySettingsMessage;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity.SecurityPermissions;

public class GuiSecurityStationPopup extends SubGuiScreen {

    private final LogisticsSecurityBlockEntity tile;
    private final SecuritySettings activeSetting;
    private GuiCheckBox cb0, cb1, cb2, cb3, cb4, cb5;

    public GuiSecurityStationPopup(SecuritySettings setting, LogisticsSecurityBlockEntity tile) {
        super(160, 135, 0, 0);
        activeSetting = setting;
        this.tile = tile;
    }

    @Override
    public void init() {
        super.init();
        cb0 = new GuiCheckBox(0, guiLeft + 138, guiTop + 26, 16, 16, false);
        cb1 = new GuiCheckBox(1, guiLeft + 138, guiTop + 41, 16, 16, false);
        cb2 = new GuiCheckBox(2, guiLeft + 138, guiTop + 56, 16, 16, false);
        cb3 = new GuiCheckBox(3, guiLeft + 138, guiTop + 71, 16, 16, false);
        cb4 = new GuiCheckBox(4, guiLeft + 138, guiTop + 86, 16, 16, false);
        cb5 = new GuiCheckBox(5, guiLeft + 138, guiTop + 101, 16, 16, false);
        cb0.setPressListener(b -> {
            activeSetting.openGui = !activeSetting.openGui;
            refreshCheckBoxes();
            sendSettings();
        });
        cb1.setPressListener(b -> {
            activeSetting.openRequest = !activeSetting.openRequest;
            refreshCheckBoxes();
            sendSettings();
        });
        cb2.setPressListener(b -> {
            activeSetting.openUpgrades = !activeSetting.openUpgrades;
            refreshCheckBoxes();
            sendSettings();
        });
        cb3.setPressListener(b -> {
            activeSetting.openNetworkMonitor = !activeSetting.openNetworkMonitor;
            refreshCheckBoxes();
            sendSettings();
        });
        cb4.setPressListener(b -> {
            activeSetting.removePipes = !activeSetting.removePipes;
            refreshCheckBoxes();
            sendSettings();
        });
        cb5.setPressListener(b -> {
            activeSetting.accessRoutingChannels = !activeSetting.accessRoutingChannels;
            refreshCheckBoxes();
            sendSettings();
        });
        addRenderableWidget(cb0);
        addRenderableWidget(cb1);
        addRenderableWidget(cb2);
        addRenderableWidget(cb3);
        addRenderableWidget(cb4);
        addRenderableWidget(cb5);
        SmallGuiButton closeBtn = new SmallGuiButton(6, guiLeft + 123, guiTop + 118, 30, 10,
            Component.translatable(Translations.Screen.CLOSE));
        closeBtn.setPressListener(b -> exitGui());
        addRenderableWidget(closeBtn);
        refreshCheckBoxes();
    }

    private void sendSettings() {
        if (activeSetting.name == null || activeSetting.name.isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new SaveSecuritySettingsMessage(
            tile.getBlockPos(), activeSetting.name, SecurityPermissions.of(activeSetting)));
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        label(guiGraphics, Component.translatable(Translations.Screen.SECURITY_STATION_PLAYER)
            .append(": " + activeSetting.name), 10);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_CONFIGURE_SETTINGS, 30);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_ACTIVE_REQUESTING, 45);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_UPGRADE_PIPES, 60);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_CHECK_NETWORK, 75);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_REMOVE_PIPES, 90);
        label(guiGraphics, Translations.Screen.SECURITY_STATION_PLAYER_ACCESS_ROUTING_CHANNELS, 105);
    }

    private void label(GuiGraphicsExtractor guiGraphics, String key, int y) {
        label(guiGraphics, Component.translatable(key).append(": "), y);
    }

    private void label(GuiGraphicsExtractor guiGraphics, Component text, int y) {
        guiGraphics.text(minecraft.font, text, guiLeft + 10, guiTop + y, 0xFF404040, false);
    }

    public void refreshCheckBoxes() {
        if (cb0 == null) {
            return;
        }
        cb0.setState(activeSetting.openGui);
        cb1.setState(activeSetting.openRequest);
        cb2.setState(activeSetting.openUpgrades);
        cb3.setState(activeSetting.openNetworkMonitor);
        cb4.setState(activeSetting.removePipes);
        cb5.setState(activeSetting.accessRoutingChannels);
    }
}
