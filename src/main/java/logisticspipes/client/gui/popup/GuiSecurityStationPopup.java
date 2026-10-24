package logisticspipes.client.gui.popup;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.Translations;
import logisticspipes.network.to_server.security.SaveSecuritySettingsMessage;
import logisticspipes.security.SecurityPermission;
import logisticspipes.security.SecurityPermissions;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

public class GuiSecurityStationPopup extends SubGuiScreen {

    private final LogisticsSecurityBlockEntity tile;
    private final String playerName;
    private final Optional<UUID> playerId;
    private SecurityPermissions permissions;
    private final Map<SecurityPermission, GuiCheckBox> checkBoxes = new EnumMap<>(SecurityPermission.class);

    public GuiSecurityStationPopup(String playerName, Optional<UUID> playerId, SecurityPermissions permissions,
        LogisticsSecurityBlockEntity tile) {
        super(160, 135, 0, 0);
        this.playerName = playerName;
        this.playerId = playerId;
        this.permissions = permissions;
        this.tile = tile;
    }

    @Override
    public void init() {
        super.init();
        checkBoxes.clear();
        for (SecurityPermission permission : SecurityPermission.values()) {
            final int row = permission.ordinal();
            GuiCheckBox checkBox = new GuiCheckBox(row, guiLeft + 138, guiTop + 26 + 15 * row, 16, 16, false);
            checkBox.setPressListener(b -> {
                permissions = permissions.with(permission, !permissions.allows(permission));
                refreshCheckBoxes();
                sendSettings();
            });
            checkBoxes.put(permission, checkBox);
            addRenderableWidget(checkBox);
        }
        SmallGuiButton closeBtn = new SmallGuiButton(6, guiLeft + 123, guiTop + 118, 30, 10,
            Component.translatable(Translations.Screen.CLOSE));
        closeBtn.setPressListener(b -> exitGui());
        addRenderableWidget(closeBtn);
        refreshCheckBoxes();
    }

    private void sendSettings() {
        if (playerName.isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(
            new SaveSecuritySettingsMessage(tile.getBlockPos(), playerName, playerId, permissions));
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        label(guiGraphics, Component.translatable(Translations.Screen.SECURITY_STATION_PLAYER)
            .append(": " + playerName), 10);
        for (SecurityPermission permission : SecurityPermission.values()) {
            label(guiGraphics, Component.translatable(permission.translationKey).append(": "),
                30 + 15 * permission.ordinal());
        }
    }

    private void label(GuiGraphicsExtractor guiGraphics, Component text, int y) {
        guiGraphics.text(minecraft.font, text, guiLeft + 10, guiTop + y, 0xFF404040, false);
    }

    public void refreshCheckBoxes() {
        checkBoxes.forEach((permission, checkBox) -> checkBox.setState(permissions.allows(permission)));
    }
}
