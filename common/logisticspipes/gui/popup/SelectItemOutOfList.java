package logisticspipes.gui.popup;

import java.util.List;
import java.util.Locale;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.ItemTooltip;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import network.rs485.logisticspipes.util.TextUtil;

public class SelectItemOutOfList extends SubGuiScreen implements IItemSearch {

	public interface IHandleItemChoice {

		void handleItemChoice(int slot);
	}

	private final List<ItemIdentifierStack> candidate;
	private final IHandleItemChoice handler;
	private ItemDisplay itemDisplay = null;
	private InputBar search;

	public SelectItemOutOfList(List<ItemIdentifierStack> candidate, IHandleItemChoice handler) {
		super(156, 188, 0, 0);
		this.candidate = candidate;
		this.handler = handler;
	}

	@Override
	public void init() {
		
		super.init();
		SmallGuiButton prev = new SmallGuiButton(0, guiLeft + 70, guiTop + 5, 10, 10, "<");
		prev.setPressListener(b -> itemDisplay.prevPage());
		addRenderableWidget(prev);
		SmallGuiButton next = new SmallGuiButton(1, guiLeft + 138, guiTop + 5, 10, 10, ">");
		next.setPressListener(b -> itemDisplay.nextPage());
		addRenderableWidget(next);
		SmallGuiButton sel = new SmallGuiButton(2, guiLeft + 100, bottom - 26, 50, 20, "Select");
		sel.setPressListener(b -> {
			ItemIdentifierStack stack = itemDisplay.getSelectedItem();
			int index = candidate.indexOf(stack);
			if (index >= 0) {
				handler.handleItemChoice(index);
			}
			exitGui();
		});
		addRenderableWidget(sel);

		if (search == null) {
			search = new InputBar(font, this.getBaseScreen(), guiLeft + 7, bottom - 23, right - guiLeft - 64, 15, false);
		}
		search.reposition(guiLeft + 7, bottom - 23, right - guiLeft - 64, 15);
        addRenderableWidget(search);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, this.getBaseScreen(), null, guiLeft + 10, guiTop + 18, xSize - 20, ySize - 48, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
			itemDisplay.setItemList(candidate);
		}
		itemDisplay.reposition(guiLeft + 8, guiTop + 18, xSize - 16, ySize - 48, 0, 0);
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
			guiGraphics.setTooltipForNextFrame(minecraft.font, tip.stack(), tip.screenX(), tip.screenY());
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	@Override
	protected void renderGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        guiGraphics.drawString(font, TextUtil.translate("misc.selectType"), guiLeft + 8, guiTop + 6, 0xFF404040, false);

		itemDisplay.renderPageNumber(guiGraphics, right - 47, guiTop + 6);

		//itemDisplay.renderSortMode(xCenter, bottom - 52);
		itemDisplay.renderItemArea(guiGraphics, 0.0f);
	}

	// Deferred: scroll wheel handling not wired

	@Override
	public boolean charTyped(CharacterEvent event) {
		char par1 = (char) event.codepoint();
		int par2 = event.modifiers();
		if (!itemDisplay.keyTyped(par1, par2)) {
			if (par2 == 1 || !search.handleKey(par1, par2)) {
				return super.charTyped(event);
			}
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (itemDisplay.handleClick((int) mouseX, (int) mouseY, button)) return true;
		if (search.handleClick((int) mouseX, (int) mouseY, button)) return true;
		return super.mouseClicked(event, doubleClick);
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
