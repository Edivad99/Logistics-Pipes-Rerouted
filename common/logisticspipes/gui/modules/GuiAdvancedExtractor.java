/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.module.AdvancedExtractorSneakyGuiPacket;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.Container;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;

public class GuiAdvancedExtractor extends ModuleBaseGui {

	private final AsyncAdvancedExtractor advancedExtractor;
	private final PropertyLayer propertyLayer;
	private final ValuePropertyOverlay<Boolean, BooleanProperty> itemsIncludedOverlay;

	public GuiAdvancedExtractor(Container playerInventory, AsyncAdvancedExtractor advancedExtractor) {
		super(buildDummy(playerInventory, advancedExtractor), advancedExtractor);
		this.advancedExtractor = advancedExtractor;

		propertyLayer = new PropertyLayer(this.advancedExtractor.getProperties());

		itemsIncludedOverlay = propertyLayer.overlay(this.advancedExtractor.getItemsIncluded());

		imageWidth = 175;
		imageHeight = 142;
	}
	private static DummyContainer buildDummy(Container playerInventory, AsyncAdvancedExtractor advancedExtractor) {
		DummyContainer dummy = new DummyContainer(playerInventory, advancedExtractor.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		//Default item toggle:
		addRenderableWidget(new GuiStringHandlerButton(0, width / 2 + 20, height / 2 - 34, 60, 20,
				() -> itemsIncludedOverlay.get() ? "Included" : "Excluded",
				() -> itemsIncludedOverlay.write(BooleanProperty::toggle)));

		logisticspipes.utils.gui.SmallGuiButton sneaky = new logisticspipes.utils.gui.SmallGuiButton(1, width / 2 - 25, height / 2 - 34, 40, 20, "Sneaky");
		sneaky.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(AdvancedExtractorSneakyGuiPacket.class).setModulePos(advancedExtractor)));
		addRenderableWidget(sneaky);
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
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(minecraft.font, advancedExtractor.getFilterInventory().getName(), 8, 6, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "Inventory", 8, imageHeight - 92, 0x404040, false);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int x, int y) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: LogisticsBaseGuiScreen.ITEMSINK
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderType::guiTextured, LogisticsBaseGuiScreen.ITEMSINK, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}

}
