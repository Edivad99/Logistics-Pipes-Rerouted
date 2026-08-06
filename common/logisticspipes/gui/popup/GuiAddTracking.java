package logisticspipes.gui.popup;

import java.util.List;
import java.util.Locale;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.blocks.stats.TrackingTask;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.AddItemToTrackPacket;
import logisticspipes.network.packets.block.RequestAmountTaskSubGui;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.ItemTooltip;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiAddTracking extends SubGuiScreen implements IItemSearch {

	private static final String PREFIX = "gui.networkstatistics.add.";

	ItemDisplay itemDisplay;
	InputBar search;
	private final LogisticsStatisticsTileEntity tile;

	public GuiAddTracking(LogisticsStatisticsTileEntity tile) {
		super(160, 200, 0, 0);
		this.tile = tile;
	}

	@Override
	public void init() {
		super.init();

		SmallGuiButton refreshBtn = new SmallGuiButton(3, guiLeft + 4, bottom - 25, 50, 20, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		SmallGuiButton addBtn = new SmallGuiButton(0, right - 55, bottom - 25, 50, 20, "Add");
		addBtn.setPressListener(b -> {
			if (itemDisplay.getSelectedItem() == null) return;
			boolean found = false;
			for (TrackingTask task : tile.tasks) {
				if (task.item.equals(itemDisplay.getSelectedItem().getItem())) {
					found = true;
					break;
				}
			}
			if (found) {
				setSubGui(new GuiMessagePopup(TextUtil.translate(PREFIX + "alreadytracked")));
			} else {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(AddItemToTrackPacket.class).setItem(itemDisplay.getSelectedItem().getItem()).setTilePos(tile));
				TrackingTask task = new TrackingTask();
				task.item = itemDisplay.getSelectedItem().getItem();
				tile.tasks.add(task);
				exitGui();
			}
		});
		addRenderableWidget(addBtn);
		SmallGuiButton nextBtn = new SmallGuiButton(1, right - 15, guiTop + 5, 10, 10, ">");
		nextBtn.setPressListener(b -> itemDisplay.nextPage());
		addRenderableWidget(nextBtn);
		SmallGuiButton prevBtn = new SmallGuiButton(2, right - 90, guiTop + 5, 10, 10, "<");
		prevBtn.setPressListener(b -> itemDisplay.prevPage());
		addRenderableWidget(prevBtn);

		SmallGuiButton sortBtn = new SmallGuiButton(20, xCenter - 13, bottom - 21, 26, 10, "Sort");
		sortBtn.setPressListener(b -> itemDisplay.cycle());
		addRenderableWidget(sortBtn);

		if (search == null) {
			search = new InputBar(font, getBaseScreen(), guiLeft + 30, bottom - 78, right - guiLeft - 58, 15);
		}
		search.reposition(guiLeft + 10, bottom - 58, right - guiLeft - 20, 15);
        addRenderableWidget(search);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, getBaseScreen(), null, guiLeft + 10, guiTop + 18, xSize - 20, ySize - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
		}
        itemDisplay.reposition(guiLeft + 10, guiTop + 18, xSize - 20, ySize - 80, 0, 0);
	}

	@Override
	public void exitGui() {
		super.exitGui();
		
		getBaseScreen().init();
	}

	@Override
	protected void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, float par3) {
		ItemTooltip tip = itemDisplay != null ? itemDisplay.getToolTip() : null;
		if (tip != null) {
			guiGraphics.renderTooltip(minecraft.font, tip.stack(), tip.screenX(), tip.screenY());
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		itemDisplay.renderItemArea(guiGraphics, 0.0f);
	}

	@Override
	protected void renderGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
		//guiGraphics.drawString(minecraft.font, StringUtil.translate(PREFIX + "title"), guiLeft + 5, guiTop + 6, 0x404040, false);
		itemDisplay.renderPageNumber(guiGraphics, right - 47, guiTop + 6);

        itemDisplay.renderSortMode(guiGraphics, xCenter, bottom - 32);
	}

	private void refreshItems() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestAmountTaskSubGui.class).setTilePos(tile));
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		itemDisplay.handleClick((int) i, (int) j, k);
		search.handleClick((int) i, (int) j, k);
		return super.mouseClicked(i, j, k);
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (i == 201) { //PgUp
			itemDisplay.prevPage();
		} else if (i == 209) { //PgDn
			itemDisplay.nextPage();
		} else {
			// Track everything except Escape when in search bar
			if (i == 1 || !search.handleKey(c, i)) {
				return super.charTyped(c, i);
			}
		}
		return true;
	}

	public void handlePacket(List<ItemIdentifierStack> identList) {
		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, getBaseScreen(), null, guiLeft + 10, guiTop + 18, xSize - 20, ySize - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
		}
		itemDisplay.setItemList(identList);
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
		ItemEnchantments enchantments = item.makeNormalStack(1)
				.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

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
}
