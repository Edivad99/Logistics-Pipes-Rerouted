package logisticspipes.gui.modules;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierInventory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import network.rs485.logisticspipes.property.StringListProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;

public class GuiOreDictItemSink extends ModuleBaseGui {

	private static ItemIdentifierInventory tmpInvStatic; // set by buildDummy before super()
	private final ItemIdentifierInventory tmpInv;
	private final PropertyLayer propertyLayer;
	private final PropertyOverlay<List<String>, StringListProperty> oreListOverlay;

	private int mouseX = 0;
	private int mouseY = 0;

	private final List<String> unsunkNames = new ArrayList<>();
	private int currentOffset = 0;
	private SmallGuiButton scrollUpButton;
	private SmallGuiButton scrollDownButton;

	// Buffered text labels populated in renderBg, drawn in renderLabels
	private final List<String> labelTexts = new ArrayList<>();
	private final List<int[]> labelPositions = new ArrayList<>(); // {x, y, color}

	public GuiOreDictItemSink(Container playerInventory, ModuleOreDictItemSink oreDictModule) {
		super(buildDummy(playerInventory, oreDictModule), oreDictModule);

		propertyLayer = new PropertyLayer(oreDictModule.getProperties());
		oreListOverlay = propertyLayer.overlay(oreDictModule.oreList);

		tmpInv = tmpInvStatic;

		imageWidth = 175;
		imageHeight = 208;
	}
	private static DummyContainer buildDummy(Container playerInventory, ModuleOreDictItemSink oreDictModule) {
		tmpInvStatic = new ItemIdentifierInventory(1, "Analyse Slot", 1);
		DummyContainer dummy = new DummyContainer(playerInventory, tmpInvStatic);
		dummy.addDummySlot(0, 7, 8);

		dummy.addNormalSlotsForPlayerInventory(7, 126);
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		scrollUpButton = new SmallGuiButton(0, leftPos + 159, topPos + 5, 10, 10, "");
		scrollUpButton.setPressListener(b -> { if (currentOffset > 0) currentOffset--; });
		addRenderableWidget(scrollUpButton);
		scrollDownButton = new SmallGuiButton(1, leftPos + 159, topPos + 17, 10, 10, "");
		scrollDownButton.setPressListener(b -> { if (currentOffset < unsunkNames.size() - 2) currentOffset++; });
		addRenderableWidget(scrollDownButton);
		scrollUpButton.active = true;
		scrollDownButton.active = true;
	}

	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			MainProxy.sendPacketToServer(ModulePropertiesUpdate.fromPropertyHolder(propertyLayer, this.minecraft.level.registryAccess()).setModulePos(module));
		}
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		int x = (int) i - leftPos;
		int y = (int) j - topPos;
		if (0 < x && x < 175 && 0 < y && y < 208) {
			mouseX = x;
			mouseY = y;
		}
		return super.mouseClicked(i, j, k);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
		int pointerX = var2 - leftPos;
		int pointerY = var3 - topPos;
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 7, topPos + 126);
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 6, topPos + 7);

		if (tmpInv.getIDStackInSlot(0) != null) {
			List<String> oreNames = getOreNames(tmpInv.getItem(0));
			oreNames.stream().filter(name -> !unsunkNames.contains(name)).forEach(unsunkNames::add);
			tmpInv.clearInventorySlotContents(0);
		}

		if (currentOffset > unsunkNames.size() - 2) currentOffset = unsunkNames.size() - 2;
		if (currentOffset < 0) currentOffset = 0;

		labelTexts.clear();
		labelPositions.clear();

		// Unsunk list: highlight bar + click handling; text buffered for renderLabels
		guiGraphics.fill(leftPos + 26, topPos + 5, leftPos + 159, topPos + 27, Color.DARK_GREY.getValue());
		final ArrayList<String> oresToAdd = oreListOverlay.read(oreList -> {
			ArrayList<String> oresToAddInner = new ArrayList<>();
			for (int i = 0; i + currentOffset < unsunkNames.size() && i < 2; i++) {
				if (27 <= pointerX && pointerX < 158 && 6 + (10 * i) <= pointerY && pointerY < 6 + (10 * (i + 1))) {
					guiGraphics.fill(leftPos + 27, topPos + 6 + (10 * i), leftPos + 158, topPos + 6 + (10 * (i + 1)), Color.LIGHT_GREY.getValue());
				}
				labelTexts.add(unsunkNames.get(currentOffset + i));
				labelPositions.add(new int[]{28, 7 + (10 * i), 0x404040});
				if (27 <= mouseX && mouseX < 158 && 6 + (10 * i) <= mouseY && mouseY < 6 + (10 * (i + 1))) {
					mouseX = 0;
					mouseY = 0;
					if (oreList.size() < 9) {
						String oreName = unsunkNames.get(currentOffset + i);
						if (!oreList.contains(oreName)) oresToAddInner.add(oreName);
						unsunkNames.remove(oreName);
					}
				}
			}
			return oresToAddInner;
		});
		if (!oresToAdd.isEmpty()) {
			oreListOverlay.write(oreList -> {
				for (String oreName : oresToAdd) {
					if (!oreList.contains(oreName)) oreList.add(oreName);
				}
				return Unit.INSTANCE;
			});
		}

		// Main ore list: highlight bar + click handling; text buffered for renderLabels
		guiGraphics.fill(leftPos + 5, topPos + 30, leftPos + 169, topPos + 122, Color.DARK_GREY.getValue());
		final ArrayList<String> oresToRemove = oreListOverlay.read(oreList -> {
			ArrayList<String> oresToRemoveInner = new ArrayList<>();
			for (int i = 0; i < oreList.size() && i < 9; i++) {
				if (6 <= pointerX && pointerX < 168 && 31 + (10 * i) <= pointerY && pointerY < 31 + (10 * (i + 1))) {
					guiGraphics.fill(leftPos + 6, topPos + 31 + (10 * i), leftPos + 168, topPos + 31 + (10 * (i + 1)), Color.LIGHT_GREY.getValue());
				}
				labelTexts.add(oreList.get(i));
				labelPositions.add(new int[]{7, 32 + (10 * i), 0x404040});
				if (6 <= mouseX && mouseX < 168 && 31 + (10 * i) <= mouseY && mouseY < 31 + (10 * (i + 1))) {
					mouseX = 0;
					mouseY = 0;
					String oreName = oreList.get(i);
					if (!unsunkNames.contains(oreName)) unsunkNames.add(oreName);
					oresToRemoveInner.add(oreName);
				}
			}
			return oresToRemoveInner;
		});
		if (!oresToRemove.isEmpty()) {
			oreListOverlay.write(oreList -> {
				for (String oreName : oresToRemove) oreList.remove(oreName);
				return Unit.INSTANCE;
			});
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		for (int i = 0; i < labelTexts.size(); i++) {
			int[] pos = labelPositions.get(i);
			guiGraphics.drawString(minecraft.font, labelTexts.get(i), pos[0], pos[1], pos[2], false);
		}
	}

	private List<String> getOreNames(ItemStack stack) {
		List<String> oreNames = new ArrayList<>();
		stack.getTags().forEach(tag -> {
			String oreName = tag.location().toString();
			if (!oreNames.contains(oreName)) {
				oreNames.add(oreName);
			}
		});
		return oreNames;
	}
}
