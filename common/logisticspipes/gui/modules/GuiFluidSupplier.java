/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleFluidSupplier;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

public class GuiFluidSupplier extends ModuleBaseGui {

	private final ModuleFluidSupplier liquidSupplier;

	public GuiFluidSupplier(Container playerInventory, ModuleFluidSupplier module) {
		super(buildDummy(playerInventory, module), module);
		liquidSupplier = module;
		imageWidth = 175;
		imageHeight = 142;
	}
	private static DummyContainer buildDummy(Container playerInventory, ModuleFluidSupplier module) {
		DummyContainer dummy = new DummyContainer(playerInventory, module.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(minecraft.font, ((logisticspipes.utils.item.ItemIdentifierInventory) liquidSupplier.getFilterInventory()).getName(), 8, 6, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "Inventory", 8, imageHeight - 92, 0x404040, false);
	}

	private static final ResourceLocation TEXTURE = LPConstants.rl("textures/gui/itemsink.png");

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int x, int y) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: GuiFluidSupplier.TEXTURE
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderType::guiTextured, GuiFluidSupplier.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}
}
