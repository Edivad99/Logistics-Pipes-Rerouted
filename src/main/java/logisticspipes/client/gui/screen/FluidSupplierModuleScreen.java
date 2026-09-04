/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleFluidSupplier;
import logisticspipes.world.inventory.SimpleFilterMenu;

public class FluidSupplierModuleScreen extends ModuleBaseScreen<SimpleFilterMenu> {

	private final ModuleFluidSupplier liquidSupplier;

	public FluidSupplierModuleScreen(SimpleFilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, menu.getModule(), 175, 142);
		liquidSupplier = (ModuleFluidSupplier) menu.getModule();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.text(minecraft.font, ((logisticspipes.utils.item.ItemIdentifierInventory) liquidSupplier.getFilterInventory()).getName(), 8, 6, 0xFF404040, false);
		guiGraphics.text(minecraft.font, "Inventory", 8, panelHeight - 92, 0xFF404040, false);
	}

	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/itemsink.png");

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
		// texture: FluidSupplierModuleScreen.TEXTURE
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FluidSupplierModuleScreen.TEXTURE, j, k, 0.0f, 0.0f, panelWidth, panelHeight, 256, 256);
	}
}
