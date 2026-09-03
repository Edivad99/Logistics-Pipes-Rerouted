package logisticspipes.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.to_server.block.OpenLogicControllerMessage;
import logisticspipes.network.to_server.pipe.OpenUpgradeConfigMessage;
import logisticspipes.network.to_server.pipe.PipeOrderWatchMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseTabGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import logisticspipes.utils.string.StringUtils;
import logisticspipes.world.inventory.PipeControllerMenu;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiPipeController extends LogisticsBaseTabGuiScreen {

	private final String PREFIX = "gui.pipecontroller.";

	private final CoreRoutedPipe pipe;

	public GuiPipeController(PipeControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 180, 220, 0, 0);
		this.pipe = menu.getPipe();

		Upgrades upgrades = new Upgrades(menu);
		Security security = new Security(menu);
		Statistics statistics = new Statistics();
		//Logic logic = new Logic();
		addHiddenSlot(menu.getDiskSlot()); //Keep it for now, but hidden. Maybe it will be used again later
		Tasks tasks = new Tasks();

		//Here order doesn't matter/can be changed to reorganise tabs
		if (LogisticsPipes.isDEBUG()) {
			addTab(upgrades);
			addTab(security);
			addTab(statistics);
			//addTab(logic);
			addTab(tasks);
		} else {
			addTab(statistics);
			addTab(upgrades);
			addTab(security);
			//addTab(logic);
			addTab(tasks);
		}
	}
	private class Upgrades extends TabSubGui {

		private final List<Slot> TAB_SLOTS_SNEAKY_INV = new ArrayList<>();
		private final Slot[] upgradeSlot = new Slot[18];
		private AbstractButton[] upgradeConfig = new AbstractButton[18];

		private Upgrades(PipeControllerMenu menu) {
			for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
				addSlot(upgradeSlot[pipeSlot] = menu.getUpgradeSlots().get(pipeSlot));
			}
			for (int pipeSlot = 9; pipeSlot < 18; pipeSlot++) {
				TAB_SLOTS_SNEAKY_INV.add(addSlot(upgradeSlot[pipeSlot] = menu.getUpgradeSlots().get(pipeSlot)));
			}
		}

		@Override
		public void initTab() {
			int x = 0;
			int y = 0;
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i] = addRenderableWidget(new SmallGuiButton(20 + i, leftPos + 13 + x, topPos + 61 + y, 10, 10, "!"));
				upgradeConfig[i].visible = pipe.getOriginalUpgradeManager().hasGuiUpgrade(i);
				x += 18;
				if (x > 160 && y == 0) {
					x = 0;
					y = 46;
				}
			}
		}

		@Override
		public void checkButton(AbstractButton button, boolean isTabActive) {
			super.checkButton(button, isTabActive);
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i].visible &= pipe.getOriginalUpgradeManager().hasGuiUpgrade(i);
			}
		}

		@Override
		public boolean showSlot(Slot slot) {
			return pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade() || !TAB_SLOTS_SNEAKY_INV.contains(slot);
		}

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			// A sneaky upgrade, the icon this tab had before the port.
			guiGraphics.item(LPItems.UPGRADE_SNEAKY.get().getDefaultInstance(), x, y);
		}

		@Override
		public void buttonClicked(AbstractButton button) {
			for (int i = 0; i < upgradeConfig.length; i++) {
				if (upgradeConfig[i] == button) {
					ClientPacketDistributor.sendToServer(new OpenUpgradeConfigMessage(upgradeSlot[i].index));
				}
			}
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {
			for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9 + pipeSlot * 18, topPos + 41);
			}
			if (pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade()) {
				for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
					LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9 + pipeSlot * 18, topPos + 87);
				}
			}
		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {
			guiGraphics.text(font, TextUtil.translate(PREFIX + "upgrade"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			if (pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade()) {
				guiGraphics.text(font, TextUtil.translate(PREFIX + "sneakyUpgrades"), 10, 74, Color.getValue(Color.DARKER_GREY), false);
			}
		}
	}

	private class Security extends TabSubGui {

		public Security(PipeControllerMenu menu) {
			addSlot(menu.getSecuritySlot());
		}

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			LPGuiGraphics.drawLockBackground(guiGraphics, x + 1, y);
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {
			LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9, topPos + 41);
		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {
			guiGraphics.text(font, TextUtil.translate(PREFIX + "security"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			ItemStack itemStack = pipe.getOriginalUpgradeManager().secInv.getItem(0);
			if (!itemStack.isEmpty()) {
				UUID id = itemStack.get(LPDataComponents.UUID);
				guiGraphics.text(font, "Id: ", 10, 68, Color.getValue(Color.DARKER_GREY), false);
				guiGraphics.text(font, ChatColor.BLUE + id.toString(), 10, 80, Color.getValue(Color.DARKER_GREY), false);
				guiGraphics.text(font, "Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? ChatColor.GREEN + "Authorized" : ChatColor.RED + "Unauthorized"), 10, 94, Color.getValue(Color.DARKER_GREY), false);
			}
		}
	}

	private class Statistics extends TabSubGui {

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			LPGuiGraphics.drawStatsBackground(guiGraphics, x, y);
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {

		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {
			String pipeName = ItemIdentifier.get(pipe.item).getFriendlyName();
			guiGraphics.text(font, pipeName, (170 - font.width(pipeName)) / 2, 28, 0xFF83601c, false);

			int sessionXCenter = 85;
			int lifetimeXCenter = 140;
			String s;

			guiGraphics.text(font, TextUtil.translate(PREFIX + "Session"), sessionXCenter - font
					.width(TextUtil.translate(PREFIX + "Session")) / 2, 40, 0xFF303030, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "Lifetime"), lifetimeXCenter - font
					.width(TextUtil.translate(PREFIX + "Lifetime")) / 2, 40, 0xFF303030, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "Sent") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Sent") + ":"), 55, 0xFF303030, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "Recieved") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Recieved") + ":"), 70, 0xFF303030, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "Relayed") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Relayed") + ":"), 85, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_sent);
			guiGraphics.text(font, s, sessionXCenter - font.width(s) / 2, 55, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_received);
			guiGraphics.text(font, s, sessionXCenter - font.width(s) / 2, 70, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_relayed);
			guiGraphics.text(font, s, sessionXCenter - font.width(s) / 2, 85, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_sent);
			guiGraphics.text(font, s, lifetimeXCenter - font.width(s) / 2, 55, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_received);
			guiGraphics.text(font, s, lifetimeXCenter - font.width(s) / 2, 70, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_relayed);
			guiGraphics.text(font, s, lifetimeXCenter - font.width(s) / 2, 85, 0xFF303030, false);

			guiGraphics.text(font, TextUtil.translate(PREFIX + "RoutingTableSize") + ":", 110 - font
					.width(TextUtil.translate(PREFIX + "RoutingTableSize") + ":"), 110, 0xFF303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.server_routing_table_size);
			guiGraphics.text(font, s, 130 - font.width(s) / 2, 110, 0xFF303030, false);
		}
	}

	private class Logic extends TabSubGui {

		private AbstractButton editButton;

		@Override
		public void initTab() {
			editButton = addRenderableWidget(new SmallGuiButton(0, leftPos + 10, topPos + 70, 160, 20, "Edit Logic Controller"));
		}

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			// A redstone torch, the icon this tab had before the port.
			guiGraphics.item(Blocks.REDSTONE_TORCH.asItem().getDefaultInstance(), x, y);
		}

		@Override
		public void buttonClicked(AbstractButton button) {
			if (button == editButton) {
				ClientPacketDistributor.sendToServer(
						new OpenLogicControllerMessage(pipe.container.getBlockPos()));
			}
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {
			guiGraphics.fill(leftPos + 12, topPos + 34, leftPos + 32, topPos + 54, Color.getValue(Color.BLACK));
			guiGraphics.fill(leftPos + 14, topPos + 36, leftPos + 30, topPos + 52, Color.getValue(Color.DARKER_GREY));
		}

		@Override
		public void checkButton(AbstractButton button, boolean isTabActive) {
			if (isTabActive) {
				button.active = pipe.container.logicController.diskInv.getItem(0) != null;
			}
			super.checkButton(button, isTabActive);
		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {

		}
	}

	private class Tasks extends TabSubGui {

		private AbstractButton leftButton;
		private AbstractButton rightButton;
		private ItemDisplay itemDisplay_5;
		private boolean managerWatching;

		@Override
		public void initTab() {
			

			leftButton = addRenderableWidget(new SmallGuiButton(1, leftPos + 95, topPos + 26, 10, 10, "<"));
			rightButton = addRenderableWidget(new SmallGuiButton(2, leftPos + 165, topPos + 26, 10, 10, ">"));
			if (itemDisplay_5 == null) {
				itemDisplay_5 = new ItemDisplay(null, font, GuiPipeController.this, null, 10, 40, 20, 60, 0, 0, 0, new int[] { 1, 1, 1, 1 }, true);
			}
			itemDisplay_5.reposition(10, 40, 20, 60, 0, 0);
		}

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			LPGuiGraphics.drawLinesBackground(guiGraphics, x, y);
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {

		}

		@Override
		public void leavingTab() {
			if (managerWatching) {
				managerWatching = false;
				ClientPacketDistributor.sendToServer(
						new PipeOrderWatchMessage(pipe.getPos(), false));
			}
		}

		@Override
		public void enteringTab() {
			if (!managerWatching) {
				managerWatching = true;
				ClientPacketDistributor.sendToServer(
						new PipeOrderWatchMessage(pipe.getPos(), true));
			}
		}

		@Override
		public void buttonClicked(AbstractButton button) {
			if (button == leftButton) {
				itemDisplay_5.prevPage();
			} else if (button == rightButton) {
				itemDisplay_5.nextPage();
			}
		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {
			List<ItemIdentifierStack> allItems = pipe.getClientSideOrderManager().stream()
					.map(IOrderInfoProvider::getAsDisplayItem).collect(Collectors.toCollection(LinkedList::new));
			itemDisplay_5.setItemList(allItems);
			itemDisplay_5.renderItemArea(guiGraphics, 0.0f);
			itemDisplay_5.renderPageNumber(guiGraphics, right - leftPos - 45, 28);
			int start = itemDisplay_5.getPage() * 3;
			int stringPos = 40;
			for (int i = start; i < start + 3 && i < pipe.getClientSideOrderManager().size(); i++) {
				IOrderInfoProvider order = pipe.getClientSideOrderManager().get(i);
				ItemIdentifier target = order.getTargetType();
				String s;
				if (target != null) {
					s = target.getFriendlyName();
					guiGraphics.text(font, s, 35, stringPos, 0xFF303030, false);
				}
				s = Integer.toString(i + 1);
				stringPos += 6;
				guiGraphics.text(font, s, 3, stringPos, 0xFF303030, false);
				stringPos += 4;
				DoubleCoordinates pos = order.getTargetPosition();
				if (pos != null) {
					s = pos.toIntBasedString();
					guiGraphics.text(font, s, 40, stringPos, 0xFF303030, false);
				}
				stringPos += 10;
			}
		}
	}
}
