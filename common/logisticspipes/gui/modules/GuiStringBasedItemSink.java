package logisticspipes.gui.modules;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.world.Container;

import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.property.StringListProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;
import javax.annotation.Nonnull;

public class GuiStringBasedItemSink extends ModuleBaseGui {

	private static ItemIdentifierInventory tmpInvStatic; // set by buildDummy before super()
	private final ItemIdentifierInventory tmpInv;
	private final PropertyLayer propertyLayer;
	private final IStringBasedModule stringBasedModule;
	private final PropertyOverlay<List<String>, StringListProperty> stringListOverlay;
	private String name = "";
	private int mouseX = 0;
	private int mouseY = 0;
	private SmallGuiButton addButton;
	private SmallGuiButton removeButton;

	public GuiStringBasedItemSink(Container playerInventory, LogisticsModule module) {
		super(buildDummy(playerInventory, module), module);
		if (!(module instanceof IStringBasedModule)) throw new IllegalArgumentException("Module must be string based");
		stringBasedModule = (IStringBasedModule) module;
		propertyLayer = new PropertyLayer(Collections.singletonList(stringBasedModule.stringListProperty()));
		stringListOverlay = propertyLayer.overlay(stringBasedModule.stringListProperty());

		tmpInv = tmpInvStatic;

		imageWidth = 175;
		imageHeight = 208;
	}
	private static DummyContainer buildDummy(Container playerInventory, LogisticsModule module) {
		tmpInvStatic = new ItemIdentifierInventory(1, "Analyse Slot", 1);
		DummyContainer dummy = new DummyContainer(playerInventory, tmpInvStatic);
		dummy.addDummySlot(0, 7, 8);

		dummy.addNormalSlotsForPlayerInventory(7, 126);
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		addButton = new SmallGuiButton(0, leftPos + 38, topPos + 18, 50, 10, "Add");
		addRenderableWidget(addButton);
		removeButton = new SmallGuiButton(1, leftPos + 107, topPos + 18, 50, 10, "Remove");
		addRenderableWidget(removeButton);
		addButton.active = false;
		removeButton.active = false;
	}

	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			MainProxy.sendPacketToServer(ModulePropertiesUpdate.fromPropertyHolder(propertyLayer).setModulePos(module));
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
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 7, topPos + 126);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 6, topPos + 7);
		SimpleGraphics.drawRectNoBlend(leftPos + 26, topPos + 5, leftPos + 169, topPos + 17, Color.DARK_GREY, 0.0);
		stringListOverlay.read(strings -> {
			final ItemIdentifierStack analyseStack = tmpInv.getIDStackInSlot(0);
			if (analyseStack != null) {
				name = "";
				guiGraphics.drawString(minecraft.font, stringBasedModule.getStringForItem(analyseStack.getItem()), leftPos + 28, topPos + 7, 0x404040);
				if (strings.contains(stringBasedModule.getStringForItem(analyseStack.getItem()))) {
					addButton.active = false;
					removeButton.active = true;
				} else if (strings.size() < 9) {
					addButton.active = true;
					removeButton.active = false;
				} else {
					addButton.active = false;
					removeButton.active = false;
				}
			} else if (name.isEmpty()) {
				addButton.active = false;
				removeButton.active = false;
			} else {
				if (strings.contains(name)) {
					guiGraphics.drawString(minecraft.font, name, leftPos + 28, topPos + 7, 0x404040);
					addButton.active = false;
					removeButton.active = true;
				} else {
					name = "";
					addButton.active = false;
					removeButton.active = false;
				}
			}
			guiGraphics.fill(leftPos + 5, topPos + 30, leftPos + 169, topPos + 122, Color.DARK_GREY.getValue());
			for (int i = 0; i < strings.size() && i < 9; i++) {
				int pointerX = var2 - leftPos;
				int pointerY = var3 - topPos;
				if (6 <= pointerX && pointerX < 168 && 31 + (10 * i) <= pointerY && pointerY < 31 + (10 * (i + 1))) {
					guiGraphics.fill(leftPos + 6, topPos + 31 + (10 * i), leftPos + 168, topPos + 31 + (10 * (i + 1)),
							Color.LIGHT_GREY.getValue());
				}
				guiGraphics.drawString(minecraft.font, strings.get(i), leftPos + 7, topPos + 32 + (10 * i), 0x404040);
				if (6 <= mouseX && mouseX < 168 && 31 + (10 * i) <= mouseY && mouseY < 31 + (10 * (i + 1))) {
					name = strings.get(i);
					mouseX = 0;
					mouseY = 0;
					tmpInv.clearInventorySlotContents(0);
				}
			}
			return null;
		});
	}
}
