/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.orderer;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import logisticspipes.LPConfigs;
import logisticspipes.gui.popup.GuiRequestPopup;
import logisticspipes.interfaces.ISpecialItemRenderer;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.RequestComponentPacket;
import logisticspipes.network.packets.orderer.RequestSubmitPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.ISubGuiController;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.ItemTooltip;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public abstract class GuiOrderer extends LogisticsBaseGuiScreen implements IItemSearch, ISpecialItemRenderer {

	public final Player entityPlayer;
	public ItemDisplay itemDisplay;
	private InputBar search;

	protected String title = "Request items";

	public final int xCoord;
	public final int yCoord;
	public final int zCoord;
	public ResourceLocation dimension;

	public static ResourceLocation dimensioncache;
	public static long cachetime;

	public GuiOrderer(int x, int y, int z, ResourceLocation dim, Player entityPlayer) {
		super(buildDummy(entityPlayer), 220, 240, 0, 0);
		xCoord = x;
		yCoord = y;
		zCoord = z;
		if (GuiOrderer.cachetime + 100 < System.currentTimeMillis()) {
			dimension = dim;
		} else {
			dimension = GuiOrderer.dimensioncache != null ? GuiOrderer.dimensioncache : dim;
		}
		this.entityPlayer = entityPlayer;
	}

	private static DummyContainer buildDummy(Player entityPlayer) {
		return new DummyContainer(entityPlayer.getInventory(), null);
	}

	public abstract void refreshItems();

	public void handlePacket(Collection<ItemIdentifierStack> allItems) {
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
			search = new InputBar(font, this, leftPos + 10, bottom - 78, imageWidth - 20, 15);
		}
		search.reposition(leftPos + 10, bottom - 78, imageWidth - 20, 15);
		addRenderableWidget(search);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, this, this, leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, xCenter, bottom - 24, 49, new int[] { 1, 10, 64, 64 }, true);
		}
		itemDisplay.reposition(leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, xCenter, bottom - 24);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);

		itemDisplay.renderPageNumber(guiGraphics, right - 47, topPos + 6);

		itemDisplay.renderAmount(guiGraphics, getStackAmount());

		itemDisplay.renderSortMode(guiGraphics, xCenter, bottom - 52);
		itemDisplay.renderItemArea(guiGraphics, 0.0f);
	}

	@Override
	public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.drawString(minecraft.font, title, minecraft.font.width(title) / 2, 6, 0x404040, false);
		if (popupCheck != null && popupCheck.getState()) {
			guiGraphics.drawString(minecraft.font, "Popup", 25, bottom - topPos - 56, 0x404040, false);
		} else {
			guiGraphics.drawString(minecraft.font, "Popup", 25, bottom - topPos - 56, Color.getValue(Color.GREY), false);
		}
	}

	@Override
	protected void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		// Deliberately not in renderLabels: that runs inside a pose translated by (leftPos, topPos),
		// which would apply the gui origin to the screen coords ItemDisplay reports.
		ItemTooltip tip = itemDisplay != null ? itemDisplay.getToolTip() : null;
		if (tip != null) {
			guiGraphics.renderTooltip(minecraft.font, tip.stack(), tip.screenX(), tip.screenY());
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
	public boolean mouseClicked(double i, double j, int k) {
		itemDisplay.handleClick((int) i, (int) j, k);
		search.handleClick((int) i, (int) j, k);
		return super.mouseClicked(i, j, k);
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
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestComponentPacket.class).setStack(stack).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
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
		MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSubmitPacket.class).setStack(stack).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (search.isFocused()) {
			if (!search.isEmpty() && search.handleKey(c, i)) {
				return true;
			}
		} else if (Screen.hasAltDown() && StringUtil.isAllowedChatCharacter(c)) {
			itemDisplay.setFocused(false);
			search.setFocused(true);
			search.setValue("");
			search.handleKey(c, i);
			return true;
		}
		if (!itemDisplay.keyTyped(c, i)) {
			// Track everything except Escape when in search bar
			if (i == 1 || !search.handleKey(c, i)) {
				return super.charTyped(c, i);
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
