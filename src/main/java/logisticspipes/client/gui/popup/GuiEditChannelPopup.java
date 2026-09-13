package logisticspipes.client.gui.popup;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.TextUtil;

public class GuiEditChannelPopup extends GuiAddChannelPopup {

    private static final String GUI_LANG_KEY = "gui.popup.editchannel.";
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
        guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "owner") + ": ", guiLeft + 10, guiTop + 115,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, toInit.getOwner().getUsername(), guiLeft + 10, guiTop + 127, 0xFF404040,
            false);
    }

    @Override
    protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"),
            (int) (xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f)), guiTop + 6,
            0xFFFFFFFF, true);
    }

}
