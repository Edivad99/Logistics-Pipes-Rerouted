package logisticspipes.client.gui.popup;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import logisticspipes.Translations;
import logisticspipes.routing.channels.ChannelInformation;

public class GuiEditChannelPopup extends GuiAddChannelPopup {

    private final UUID channelIdentifier;
    private final ChannelInformation toInit;

    public GuiEditChannelPopup(UUID correspondingSecurityStationID, ChannelInformation toEdit) {
        super(correspondingSecurityStationID, 160);
        this.channelIdentifier = toEdit.getChannelIdentifier();
        toInit = toEdit;
    }

    @Override
    protected Optional<UUID> channelToSave() {
        return Optional.of(channelIdentifier);
    }

    @Override
    protected int saveButtonY() {
        return guiTop + 140;
    }

    @Override
    public void init() {
        super.init();
        if (toInit != null) {
            // A channel need not have a name, and EditBox refuses null: an unnamed one opens empty.
            this.textInput.setValue(Objects.requireNonNullElse(toInit.getName(), ""));
            checkPublic.setState(toInit.getRights() == ChannelInformation.AccessRights.PUBLIC);
            checkSecurity.setState(toInit.getRights() == ChannelInformation.AccessRights.SECURED);
            checkPrivate.setState(toInit.getRights() == ChannelInformation.AccessRights.PRIVATE);
        }
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractGuiBackground(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font, Component.translatable(Translations.Screen.CHANNEL_OWNER).append(": "),
            guiLeft + 10, guiTop + 115, 0xFF404040, false);
        guiGraphics.text(minecraft.font, toInit.getOwner().getUsername(), guiLeft + 10, guiTop + 127, 0xFF404040,
            false);
    }

    @Override
    protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.centeredText(minecraft.font, Component.translatable(Translations.Screen.CHANNEL_EDIT_TITLE),
            xCenter, guiTop + 6, 0xFFFFFFFF);
    }

}
