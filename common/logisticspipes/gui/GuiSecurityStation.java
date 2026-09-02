package logisticspipes.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSecurityTileEntity.CardAction;
import logisticspipes.blocks.LogisticsSecurityTileEntity.SecurityFlag;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.gui.popup.GuiEditCCAccessTable;
import logisticspipes.gui.popup.GuiSecurityStationPopup;
import logisticspipes.interfaces.PlayerListReciver;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.OpenSecurityChannelManagerPacket;
import logisticspipes.network.to_server.security.OpenSecurityPlayerMessage;
import logisticspipes.network.to_server.security.RequestPlayerListMessage;
import logisticspipes.network.to_server.security.RequestSecurityStationCCIdsMessage;
import logisticspipes.network.to_server.security.SecurityCardActionMessage;
import logisticspipes.network.to_server.security.SetSecurityStationAuthorizedMessage;
import logisticspipes.network.to_server.security.ToggleSecurityStationFlagMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.world.inventory.SecurityStationMenu;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSecurityStation extends LogisticsBaseGuiScreen implements PlayerListReciver {

	private static final String PREFIX = "gui.securitystation.";

	private final LogisticsSecurityTileEntity tile;
	private final List<String> players = new LinkedList<>();

	//Player name:
	protected static final int searchWidth = 250;
	protected int lastClickedX = 0;
	protected int lastClickedY = 0;
	protected int lastClickedK = 0;
	private int addition;
	private boolean authorized;
	private InputBar searchBar;
	private SmallGuiButton btnMinusMinus;
	private SmallGuiButton btnMinus;
	private SmallGuiButton btnPlus;
	private SmallGuiButton btnPlusPlus;
	private SmallGuiButton btnOpen;
	private GuiCheckBox checkAllowCC;
	private SmallGuiButton btnEditTable;
	private SmallGuiButton btnAuthorize;
	private SmallGuiButton btnDeauthorize;
	private GuiCheckBox checkAutoDestroy;
	private SmallGuiButton btnChannelManager;

	protected final String title = "Request items";
	protected boolean clickWasButton = false;

	public GuiSecurityStation(SecurityStationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 280, 260, 0, 0);
		this.tile = menu.getBlockEntity();
		authorized = SimpleServiceLocator.securityStationManager.isAuthorized(tile.getSecId());
	}


	@Override
	public void init() {
		super.init();
		btnMinusMinus = new SmallGuiButton(0, leftPos + 10, topPos + 179, 30, 20, "--");
		btnMinusMinus.setPressListener(b -> ClientPacketDistributor.sendToServer(new SecurityCardActionMessage(tile.getBlockPos(), CardAction.CLEAR)));
		addRenderableWidget(btnMinusMinus);
		btnMinusMinus.visible = false;
		btnMinus = new SmallGuiButton(1, leftPos + 10, topPos + 139, 30, 20, "-");
		btnMinus.setPressListener(b -> ClientPacketDistributor.sendToServer(new SecurityCardActionMessage(tile.getBlockPos(), CardAction.TAKE_ONE)));
		addRenderableWidget(btnMinus);
		btnPlus = new SmallGuiButton(2, leftPos + 45, topPos + 139, 30, 20, "+");
		btnPlus.setPressListener(b -> ClientPacketDistributor.sendToServer(new SecurityCardActionMessage(tile.getBlockPos(), CardAction.GIVE_ONE)));
		addRenderableWidget(btnPlus);
		btnPlusPlus = new SmallGuiButton(3, leftPos + 140, topPos + 179, 30, 20, "++");
		btnPlusPlus.setPressListener(b -> ClientPacketDistributor.sendToServer(new SecurityCardActionMessage(tile.getBlockPos(), CardAction.GIVE_STACK)));
		addRenderableWidget(btnPlusPlus);
		btnPlusPlus.visible = false;
		btnOpen = new SmallGuiButton(4, leftPos + 241, topPos + 217, 30, 10, TextUtil.translate(GuiSecurityStation.PREFIX + "Open"));
		btnOpen.setPressListener(b -> {
			if (!searchBar.getValue().isEmpty()) {
				ClientPacketDistributor.sendToServer(
					new OpenSecurityPlayerMessage(tile.getBlockPos(), searchBar.getValue()));
			}
		});
		addRenderableWidget(btnOpen);
		checkAllowCC = new GuiCheckBox(5, leftPos + 160, topPos + 42, 16, 16, tile.allowCC);
		checkAllowCC.setPressListener(b -> {
			tile.allowCC = !tile.allowCC;
			refreshCheckBoxes();
			ClientPacketDistributor.sendToServer(
				new ToggleSecurityStationFlagMessage(tile.getBlockPos(), SecurityFlag.ALLOW_CC));
		});
		addRenderableWidget(checkAllowCC);
		btnEditTable = new SmallGuiButton(6, leftPos + 162, topPos + 60, 60, 10, TextUtil.translate(GuiSecurityStation.PREFIX + "EditTable"));
		btnEditTable.setPressListener(b -> {
			setSubGui(new GuiEditCCAccessTable(tile));
			ClientPacketDistributor.sendToServer(new RequestSecurityStationCCIdsMessage(tile.getBlockPos()));
		});
		addRenderableWidget(btnEditTable);
		// ComputerCraft not available on 1.20.1 (former dummy isCC() was always false) — CC widgets only in DEBUG.
		if (!LogisticsPipes.isDEBUG()) {
			checkAllowCC.visible = false;
			btnEditTable.visible = false;
		}
		btnAuthorize = new SmallGuiButton(7, leftPos + 55, topPos + 95, 70, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "Authorize"));
		btnAuthorize.setPressListener(b -> {
			ClientPacketDistributor.sendToServer(
				new SetSecurityStationAuthorizedMessage(tile.getBlockPos(), true));
			authorized = true;
		});
		addRenderableWidget(btnAuthorize);
		btnDeauthorize = new SmallGuiButton(8, leftPos + 175, topPos + 95, 70, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "Deauthorize"));
		btnDeauthorize.setPressListener(b -> {
			ClientPacketDistributor.sendToServer(
				new SetSecurityStationAuthorizedMessage(tile.getBlockPos(), false));
			authorized = false;
		});
		addRenderableWidget(btnDeauthorize);
		checkAutoDestroy = new GuiCheckBox(9, leftPos + 160, topPos + 74, 16, 16, tile.allowAutoDestroy);
		checkAutoDestroy.setPressListener(b -> {
			tile.allowAutoDestroy = !tile.allowAutoDestroy;
			refreshCheckBoxes();
			ClientPacketDistributor.sendToServer(
				new ToggleSecurityStationFlagMessage(tile.getBlockPos(), SecurityFlag.AUTO_DESTROY));
		});
		addRenderableWidget(checkAutoDestroy);
		btnChannelManager = new SmallGuiButton(10, leftPos + 177, topPos + 230, 95, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "ChannelManager"));
		btnChannelManager.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenSecurityChannelManagerPacket.class).setBlockPos(tile.getBlockPos())));
		addRenderableWidget(btnChannelManager);
		if (searchBar == null) {
			searchBar = new InputBar(this.font, this, leftPos + 180, bottom - 120, right - 8 + addition - leftPos - 180, 17);
			lastClickedX = -10000000;
			lastClickedY = -10000000;
		}
		searchBar.reposition(leftPos + 180, bottom - 120, right - 8 + addition - leftPos - 180, 17);
        addRenderableWidget(searchBar);
		ClientPacketDistributor.sendToServer(new RequestPlayerListMessage());
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 175);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 81, topPos + 140);

		addition = (minecraft.font.width(searchBar.getValue()) - 82);
		if (addition < 0) addition = 0;

        // Click detection for player list (drawing happens in extractLabels)
		int pos = bottom - 95;
		for (String player : players) {
			if (player.contains(searchBar.getValue())) {
				pos += 11;
			}
			if (leftPos + 180 < lastClickedX && lastClickedX < leftPos + 280 && pos - 11 < lastClickedY && lastClickedY < pos) {
				lastClickedX = -10000000;
				lastClickedY = -10000000;
				searchBar.setValue(player);
			}
			if (pos > bottom - 12) break;
		}

		if (authorized) {
			guiGraphics.fill(leftPos + 127, topPos + 101, leftPos + 147, topPos + 108, Color.getValue(Color.GREEN));
		} else {
			guiGraphics.fill(leftPos + 153, topPos + 101, leftPos + 173, topPos + 108, Color.getValue(Color.RED));
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "SecurityStation"), 105, 10, 0xFF404040, false);
		guiGraphics.text(font, tile.getSecId() == null ? "null" : tile.getSecId().toString(), 32, 25, 0xFF404040, false);
		if (LogisticsPipes.isDEBUG()) {
			guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "allowCCAccess") + ":", 10, 46, 0xFF404040, false);
			guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "excludeIDs") + ":", 10, 61, 0xFF404040, false);
		}
		guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "pipeRemove") + ":", 10, 78, 0xFF404040, false);
		guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "Player") + ":", 180, 127, 0xFF404040, false);
		guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "SecurityCards") + ":", 10, 127, 0xFF404040, false);
		guiGraphics.text(font, TextUtil.translate(GuiSecurityStation.PREFIX + "Inventory") + ":", 10, 163, 0xFF404040, false);

		int pos = bottom - topPos - 95;
		for (String player : players) {
			if (player.contains(searchBar.getValue())) {
				guiGraphics.text(font, player, 180, pos, 0xFF404040, false);
				pos += 11;
			}
			if (pos > bottom - topPos - 12) {
				guiGraphics.text(font, "...", 180, pos - 5, 0xFF404040, false);
				break;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double i = event.x();
		double j = event.y();
		int k = event.button();
		if ((i >= leftPos + 5 && i < right - 5 + addition && j >= topPos + 5 && j < bottom - 5) && !searchBar.isFocused()) {
			lastClickedX = (int)i;
			lastClickedY = (int)j;
			lastClickedK = k;
		}
		if (searchBar.handleClick(i, j, k)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char c = (char) event.codepoint();
		int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (searchBar.isFocused()) {
			if ((c == 13) || (i == 1) || (i == 28)) {
				searchBar.setFocused(false);
				return true;
			}
			if (searchBar.handleKey(c, i)) {
				return true;
			}
		}
		return super.charTyped(event);
	}

	@Override
	public void receivePlayerList(List<String> list) {
		players.clear();
		players.addAll(list);
	}

	public void handlePlayerSecurityOpen(SecuritySettings setting) {
		searchBar.setValue("");
		setSubGui(new GuiSecurityStationPopup(setting, tile));
	}

	public void refreshCheckBoxes() {
		checkAllowCC.setState(tile.allowCC);
		checkAutoDestroy.setState(tile.allowAutoDestroy);
	}
}
