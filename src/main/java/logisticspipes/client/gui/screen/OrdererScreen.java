/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import logisticspipes.LPConfigs;
import logisticspipes.client.gui.popup.GuiRequestPopup;
import logisticspipes.interfaces.IAvailableItemsReceiver;
import logisticspipes.interfaces.ISpecialItemRenderer;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.network.to_server.orderer.SimulateRequestMessage;
import logisticspipes.network.to_server.orderer.SubmitRequestMessage;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.ISubGuiController;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.ItemTooltip;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.inventory.OrdererMenu;

public abstract class OrdererScreen<T extends OrdererMenu> extends LogisticsBaseGuiScreen<T> implements IItemSearch, ISpecialItemRenderer, IAvailableItemsReceiver {

	public final Player entityPlayer;
	public ItemDisplay itemDisplay;
	private InputBar search;

	protected String title = "Request items";

	public final int xCoord;
	public final int yCoord;
	public final int zCoord;
	public Identifier dimension;

	public OrdererScreen(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 220, 240, 0, 0);
		final BlockPos pos = menu.getTarget().pos();
		xCoord = pos.getX();
		yCoord = pos.getY();
		zCoord = pos.getZ();
		dimension = menu.getTarget().dimension();
		entityPlayer = inventory.player;
	}

	public abstract void refreshItems();

	@Override
	public void setAvailableItems(Collection<ItemIdentifierStack> allItems) {
		itemDisplay.setItemList(allItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
	}

	@Override
	public void init() {
		

		super.init();

		// super.init() → rebuildWidgets() already cleared prior widgets
		addRenderableWidget(wire(new SmallGuiButton(0, right - 55, bottom - 25, 50, 20, "Request"), 0)); // Request
		addRenderableWidget(wire(new SmallGuiButton(1, right - 15, topPos + 5, 10, 10, ">"), 1)); // Next page
		addRenderableWidget(wire(new SmallGuiButton(2, right - 90, topPos + 5, 10, 10, "<"), 2)); // Prev page
		addRenderableWidget(wire(new SmallGuiButton(10, xCenter - 51, bottom - 15, 26, 10, "---"), 10)); // -64
		addRenderableWidget(wire(new SmallGuiButton(4, xCenter - 51, bottom - 26, 15, 10, "--"), 4)); // -10
		addRenderableWidget(wire(new SmallGuiButton(5, xCenter - 35, bottom - 26, 10, 10, "-"), 5)); // -1
		addRenderableWidget(wire(new SmallGuiButton(6, xCenter + 26, bottom - 26, 10, 10, "+"), 6)); // +1
		addRenderableWidget(wire(new SmallGuiButton(7, xCenter + 38, bottom - 26, 15, 10, "++"), 7)); // +10
		addRenderableWidget(wire(new SmallGuiButton(11, xCenter + 26, bottom - 15, 26, 10, "+++"), 11)); // +64
		popupCheck = new GuiCheckBox(8, leftPos + 9, bottom - 60, 14, 14, LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean());
		popupCheck.setPressListener(b -> handleBtn(8, b));
		addRenderableWidget(popupCheck); // Popup

		addRenderableWidget(wire(new SmallGuiButton(20, xCenter - 13, bottom - 41, 26, 10, "Sort"), 20)); // Sort

		if (search == null) {
			search = new InputBar(font, this, leftPos + 10, bottom - 78, panelWidth - 20, 15);
		}
		search.reposition(leftPos + 10, bottom - 78, panelWidth - 20, 15);
		addRenderableWidget(search);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, this, this, leftPos + 10, topPos + 18, panelWidth - 20, panelHeight - 100, xCenter, bottom - 24, 49, new int[] { 1, 10, 64, 64 }, true);
		}
		itemDisplay.reposition(leftPos + 10, topPos + 18, panelWidth - 20, panelHeight - 100, xCenter, bottom - 24);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);

		itemDisplay.renderPageNumber(guiGraphics, right - 47, topPos + 6);

		itemDisplay.renderAmount(guiGraphics, getStackAmount());

		itemDisplay.renderSortMode(guiGraphics, xCenter, bottom - 52);
		itemDisplay.renderItemArea(guiGraphics, 0.0f);
	}

	@Override
	public void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.text(minecraft.font, title, minecraft.font.width(title) / 2, 6, 0xFF404040, false);
		if (popupCheck != null && popupCheck.getState()) {
			guiGraphics.text(minecraft.font, "Popup", 25, bottom - topPos - 56, 0xFF404040, false);
		} else {
			guiGraphics.text(minecraft.font, "Popup", 25, bottom - topPos - 56, Color.getValue(Color.GREY), false);
		}
	}

	@Override
	protected void renderToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		// Deliberately not in extractLabels: that runs inside a pose translated by (leftPos, topPos),
		// which would apply the gui origin to the screen coords ItemDisplay reports.
		ItemTooltip tip = itemDisplay != null ? itemDisplay.getToolTip() : null;
		if (tip != null) {
			guiGraphics.setTooltipForNextFrame(minecraft.font, tip.stack(), tip.screenX(), tip.screenY());
		}
	}

	@Override
	public boolean itemSearched(ItemIdentifier item) {
		if (search.isEmpty()) {
			return true;
		}
		if (isSearched(item.getFriendlyName().toLowerCase(Locale.US), search.getValue().toLowerCase(Locale.US))) {
			return true;
		}
		//if(isSearched(String.valueOf(BuiltInRegistries.ITEM.getId(item.item)), search.getContent())) return true;
		//Enchantment? Enchantment!
		ItemEnchantments enchantments =
				item.makeNormalStack(1).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			String enchantName = entry.getKey().value().description().getString();

			if (isSearched(
					enchantName.toLowerCase(Locale.US),
					search.getValue().toLowerCase(Locale.US))) {
				return true;
			}
		}
		return false;
	}

	private boolean isSearched(String value, String search) {
		boolean flag = true;
		for (String s : search.split(" ")) {
			if (!value.contains(s)) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double i = event.x();
		double j = event.y();
		int k = event.button();
		itemDisplay.handleClick((int) i, (int) j, k);
		search.handleClick((int) i, (int) j, k);
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		itemDisplay.handleMouse(scrollY);
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	public void handleRequestAnswer(Collection<IResource> items, boolean error, ISubGuiController control, Player player) {
		while (control.hasSubGui()) {
			control = control.getSubGui();
		}
		if (error) {
			control.setSubGui(new GuiRequestPopup(entityPlayer, "You are missing:", items));
		} else {
			control.setSubGui(new GuiRequestPopup(entityPlayer, "Request successful!", items));
		}
	}

	public void handleSimulateAnswer(Collection<IResource> used, Collection<IResource> missing, ISubGuiController control, Player player) {
		while (control.hasSubGui()) {
			control = control.getSubGui();
		}
		control.setSubGui(new GuiRequestPopup(entityPlayer, "Components: ", used, "Missing: ", missing));
	}

	private GuiCheckBox popupCheck;

	private SmallGuiButton wire(SmallGuiButton btn, int id) {
		btn.setPressListener(b -> handleBtn(id, b));
		return btn;
	}

	private void handleBtn(int id, AbstractButton guibutton) {
		if (id == 0 && itemDisplay.getSelectedItem() != null) {
			submitRequest();
			refreshItems();
		} else if (id == 1) {
			itemDisplay.nextPage();
		} else if (id == 2) {
			itemDisplay.prevPage();
		} else if (id == 3) {
			refreshItems();
		} else if (id == 10) {
			itemDisplay.sub(3);
		} else if (id == 4) {
			itemDisplay.sub(2);
		} else if (id == 5) {
			itemDisplay.sub(1);
		} else if (id == 6) {
			itemDisplay.add(1);
		} else if (id == 7) {
			itemDisplay.add(2);
		} else if (id == 11) {
			itemDisplay.add(3);
		} else if (id == 8) {
			GuiCheckBox button = (GuiCheckBox) guibutton;
			LPConfigs.COMMON.DISPLAY_POPUP.set(button.change());
			LPConfigs.savePopupState();
		} else if (id == 13 && itemDisplay.getSelectedItem() != null) {
			final ItemIdentifierStack stack = itemDisplay.getSelectedItem().getItem().makeStack(itemDisplay.getRequestCount());
			ClientPacketDistributor.sendToServer(new SimulateRequestMessage(
					new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord)), stack));
		} else if (id == 20) {
			itemDisplay.cycle();
		}

		// super.actionPerformed removed — no such method in 1.20.1 Screen
	}

	protected int getStackAmount() {
		return 64;
	}

	/**
	 * Sends the request for the currently selected item. Called by the shared "Request" button, which
	 * only fires when something is selected; subclasses override this to send their own packet type
	 * instead of adding a second button.
	 */
	protected void submitRequest() {
		final ItemIdentifierStack stack = itemDisplay.getSelectedItem().getItem().makeStack(itemDisplay.getRequestCount());
		ClientPacketDistributor.sendToServer(new SubmitRequestMessage(
				new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord)), stack));
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char c = (char) event.codepoint();
		int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (search.isFocused()) {
			if (!search.isEmpty() && search.handleKey(c, i)) {
				return true;
			}
		} else if (Minecraft.getInstance().hasAltDown() && StringUtil.isAllowedChatCharacter(c)) {
			itemDisplay.setFocused(false);
			search.setFocused(true);
			search.setValue("");
			search.handleKey(c, i);
			return true;
		}
		if (!itemDisplay.keyTyped(c, i)) {
			// Track everything except Escape when in search bar
			if (i == 1 || !search.handleKey(c, i)) {
				return super.charTyped(event);
			}
		}
		return false;
	}

	@Override
	public void resetSubGui() {
		super.resetSubGui();
		refreshItems();
	}
}
