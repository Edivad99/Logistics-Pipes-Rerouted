package logisticspipes.gui.popup;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.AddNewChannelPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiAddChannelPopup extends SubGuiScreen {

	private static String GUI_LANG_KEY = "gui.popup.addchannel.";
	protected InputBar textInput = null;
	protected final UUID responsibleSecurityID;
	protected GuiCheckBox checkPublic = null;
	protected GuiCheckBox checkSecurity = null;
	protected GuiCheckBox checkPrivate = null;

	public GuiAddChannelPopup(UUID responsibleSecurityID) {
		super(118, 140, 0, 0);
		this.responsibleSecurityID = responsibleSecurityID;
	}

	protected GuiAddChannelPopup(UUID responsibleSecurityID, int panelHeight) {
		super(118, panelHeight, 0, 0);
		this.responsibleSecurityID = responsibleSecurityID;
	}

	@Override
	public void init() {


		super.init();

		checkPublic = new GuiCheckBox(0, guiLeft + 94, guiTop + 66, 16, 16, true);
		checkSecurity = new GuiCheckBox(1, guiLeft + 94, guiTop + 81, 16, 16, false);
		checkPrivate = new GuiCheckBox(2, guiLeft + 94, guiTop + 96, 16, 16, false);
		checkPublic.setPressListener(b -> { checkPublic.setState(true); checkSecurity.setState(false); checkPrivate.setState(false); });
		checkSecurity.setPressListener(b -> { checkPublic.setState(false); checkSecurity.setState(true); checkPrivate.setState(false); });
		checkPrivate.setPressListener(b -> { checkPublic.setState(false); checkSecurity.setState(false); checkPrivate.setState(true); });
		addRenderableWidget(checkPublic);
		addRenderableWidget(checkSecurity);
		addRenderableWidget(checkPrivate);

		SmallGuiButton saveBtn = new SmallGuiButton(4, guiLeft + 58, guiTop + 120, 50, 10, TextUtil.translate(GUI_LANG_KEY + "save"));
		saveBtn.setPressListener(b -> {
			ChannelInformation.AccessRights rights = null;
			UUID security = null;
			if (checkPublic.getState()) {
				rights = ChannelInformation.AccessRights.PUBLIC;
			} else if (checkSecurity.getState()) {
				rights = ChannelInformation.AccessRights.SECURED;
				security = responsibleSecurityID;
			} else if (checkPrivate.getState()) {
				rights = ChannelInformation.AccessRights.PRIVATE;
			}
			MainProxy.sendPacketToServer(PacketHandler.getPacket(AddNewChannelPacket.class).setName(this.textInput.getValue()).setRights(rights).setSecurityStationID(security));
			exitGui();
		});
		addRenderableWidget(saveBtn);

		if (this.textInput == null) {
			this.textInput = new InputBar(Minecraft.getInstance().font, this.getBaseScreen(), guiLeft + 30, guiTop + 32, right - guiLeft - 20, 15);
		}
		this.textInput.reposition(guiLeft + 10, guiTop + 34, right - guiLeft - 20, 15);
        addRenderableWidget(this.textInput);

		checkSecurity.active = responsibleSecurityID != null;
	}

	@Override
	public void exitGui() {
		super.exitGui();

		getBaseScreen().init();
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
		drawTitle(guiGraphics);
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "name"), guiLeft + 10, guiTop + 20, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "access") + ":", guiLeft + 10, guiTop + 55, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "public"), guiLeft + 10, guiTop + 70, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "security"), guiLeft + 10, guiTop + 85, responsibleSecurityID != null ? 0xFF404040 : 0xFF808080, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "private"), guiLeft + 10, guiTop + 100, 0xFF404040, false);
	}

	protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), xCenter - minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2, guiTop + 6, 0xFFFFFFFF, true);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char par1 = (char) event.codepoint();
		int par2 = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (!this.textInput.handleKey(par1, par2)) {
			return super.charTyped(event);
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int mouseButton = event.button();
		if (!this.textInput.handleClick((int) mouseX, (int) mouseY, mouseButton)) {
			return super.mouseClicked(event, doubleClick);
		}
		return true;
	}

}
