/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.module.SetModulePropertiesMessage;
import logisticspipes.network.to_server.crafting.SlotFinderOpenGuiMessage;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.world.inventory.ActiveSupplierMenu;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;
import network.rs485.logisticspipes.util.TextUtil;

public class SupplierPipeScreen extends LogisticsBaseGuiScreen<ActiveSupplierMenu> {

	private static final String PREFIX = "gui.supplierpipe.";
	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/supplier.png");
	private final boolean hasPatternUpgrade;
	private final PropertyLayer propertyLayer;
	private final ModuleActiveSupplier supplierModule;
	private final PropertyOverlay<List<Integer>, IntListProperty> slotAssignmentPatternOverlay;
	private final ValuePropertyOverlay<SupplyMode, EnumProperty<SupplyMode>> requestModeOverlay;
	private final ValuePropertyOverlay<PatternMode, EnumProperty<PatternMode>> patternModeOverlay;
	private ValuePropertyOverlay<Boolean, BooleanProperty> limitedPropertyOverlay;
	private SmallGuiButton modeBtn;
	private SmallGuiButton limBtn;

	public SupplierPipeScreen(ActiveSupplierMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 194, 186, 0, 0);
		hasPatternUpgrade = menu.isPatternUpgrade();
		supplierModule = menu.getSupplier();

		propertyLayer = new PropertyLayer(supplierModule.getProperties());
		slotAssignmentPatternOverlay = propertyLayer.overlay(supplierModule.slotAssignmentPattern);
		patternModeOverlay = propertyLayer.overlay(supplierModule.patternMode);
		requestModeOverlay = propertyLayer.overlay(supplierModule.requestMode);
		limitedPropertyOverlay = propertyLayer.overlay(supplierModule.isLimited);
	}

	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			ClientPacketDistributor.sendToServer(SetModulePropertiesMessage.of(
					ModuleTarget.of(supplierModule), propertyLayer,
					Minecraft.getInstance().level.registryAccess()));
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		String name;
		if (hasPatternUpgrade) {
			name = TextUtil.translate(SupplierPipeScreen.PREFIX + "TargetInvPattern");
		} else {
			name = TextUtil.translate(SupplierPipeScreen.PREFIX + "TargetInv");
		}
		guiGraphics.text(minecraft.font, name, panelWidth / 2 - minecraft.font.width(name) / 2, 6, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(SupplierPipeScreen.PREFIX + "Inventory"), 18, panelHeight - 102, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(SupplierPipeScreen.PREFIX + "RequestMode"), panelWidth - 140, panelHeight - 112, 0xFF404040, false);
		if (hasPatternUpgrade) {
			slotAssignmentPatternOverlay.read((slotAssignments) -> {
				for (int i = 0; i < slotAssignments.size(); i++) {
					guiGraphics.text(minecraft.font, Integer.toString(slotAssignments.get(i)), 22 + i * 18, 55, 0xFF404040, false);
				}
				return null;
			});
		}
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
		if (!hasPatternUpgrade) {
			// texture: SupplierPipeScreen.TEXTURE
			int j = leftPos;
			int k = topPos;
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SupplierPipeScreen.TEXTURE, j, k, 0.0f, 0.0f, panelWidth, panelHeight, 256, 256);
		} else {
			LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
			for (int i = 0; i < 9; i++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 17 + i * 18, topPos + 19);
				Slot slot = getMenu().getSlot(36 + i);
				if (slot.hasItem() && slot.getItem().getCount() > 64) {
					guiGraphics.fill(leftPos + 18 + i * 18, topPos + 20, leftPos + 34 + i * 18, topPos + 36, Color.getValue(Color.RED));
				}
			}
			LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 18, topPos + 97);
		}
	}

	@Override
	public void init() {
		super.init();
		modeBtn = new SmallGuiButton(0, width / 2 + 35, height / 2 - 25, 50, 20, getModeText());
		modeBtn.setPressListener(b -> {
			if (hasPatternUpgrade) {
				final PatternMode newMode = patternModeOverlay.write(EnumProperty::next);
				modeBtn.setMessage(Component.literal(newMode.toString()));
			} else {
				final SupplyMode newMode = requestModeOverlay.write(EnumProperty::next);
				modeBtn.setMessage(Component.literal(newMode.toString()));
			}
		});
		addRenderableWidget(modeBtn);
		if (hasPatternUpgrade) {
			limBtn = new SmallGuiButton(1, leftPos + 5, topPos + 68, 45, 10, getLimitationText());
			limBtn.setPressListener(b -> {
				limitedPropertyOverlay.write(BooleanProperty::toggle);
				limBtn.setMessage(Component.literal(getLimitationText()));
			});
			addRenderableWidget(limBtn);
			for (int i = 0; i < 9; i++) {
				final int slot = i;
				SmallGuiButton setBtn = new SmallGuiButton(i + 2, leftPos + 18 + i * 18, topPos + 40, 17, 10, "Set");
				setBtn.setPressListener(b -> ClientPacketDistributor.sendToServer(
						new SlotFinderOpenGuiMessage(ModuleTarget.of(supplierModule), slot)));
				addRenderableWidget(setBtn);
			}
		}
	}

	public void refreshMode() {
		if (modeBtn != null) modeBtn.setMessage(Component.literal(getModeText()));
		if (hasPatternUpgrade) {
			limitedPropertyOverlay = propertyLayer.overlay(supplierModule.isLimited);
			if (limBtn != null) limBtn.setMessage(Component.literal(getLimitationText()));
		}
	}

	private String getLimitationText() {
		return limitedPropertyOverlay.get() ? "Limited" : "Unlimited";
	}

	private String getModeText() {
		return (hasPatternUpgrade ? patternModeOverlay : requestModeOverlay).get().toString();
	}

}
