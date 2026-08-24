/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.LPConstants;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.item.ItemIdentifierInventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import network.rs485.logisticspipes.module.SimpleFilter;

public class GuiSimpleFilter extends ModuleBaseGui {

	private final SimpleFilter filter;

	public GuiSimpleFilter(Container playerInventory, LogisticsModule filterModule) {
		super(buildDummy(playerInventory, filterModule), filterModule);
		if (!(filterModule instanceof SimpleFilter)) throw new IllegalArgumentException("Module is not a filter module");
		filter = (SimpleFilter) filterModule;
		imageWidth = 175;
		imageHeight = 142;
	}
	private static DummyContainer buildDummy(Container playerInventory, LogisticsModule filterModule) {
		SimpleFilter f = (filterModule instanceof SimpleFilter) ? (SimpleFilter) filterModule : null;
		DummyContainer dummy = new DummyContainer(playerInventory, f != null ? f.getFilterInventory() : null);
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(minecraft.font, ((ItemIdentifierInventory) filter.getFilterInventory()).getName(), 8, 6, 0xFF404040, false);
		guiGraphics.drawString(minecraft.font, "Inventory", 8, imageHeight - 92, 0xFF404040, false);
	}

	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/itemsink.png");

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int x, int y) {
		// texture: GuiSimpleFilter.TEXTURE
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiSimpleFilter.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}
}
