/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.item;

import java.util.List;

import javax.annotation.Nullable;

import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.SimpleGraphics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import network.rs485.logisticspipes.util.TextUtil;

@Data
@Accessors(chain = true)
@OnlyIn(Dist.CLIENT)
public class ItemStackRenderer {

	private TextureManager texManager;
	private Font font;

	private ItemStack itemstack = ItemStack.EMPTY;
    @Nullable
	private ItemIdentifierStack itemIdentStack;
	private int posX;
	private int posY;
	private float zLevel;
	private float scaleX;
	private float scaleY;
	private float scaleZ;
	private DisplayAmount displayAmount;
	private boolean renderEffects;
	private boolean ignoreDepth;
	private boolean renderInColor;
	private ItemEntity entityitem;
    @Nullable
	private Level world;
	private float partialTickTime;

	public ItemStackRenderer(int posX, int posY, float zLevel, boolean renderEffects, boolean ignoreDepth) {
		this.posX = posX;
		this.posY = posY;
		this.zLevel = zLevel;
		this.renderEffects = renderEffects;
		this.ignoreDepth = ignoreDepth;
		font = Minecraft.getInstance().font;
		world = null;
		texManager = Minecraft.getInstance().getTextureManager();
		scaleX = 1.0F;
		scaleY = 1.0F;
		scaleZ = 1.0F;
	}

	public static void renderItemIdentifierStackListIntoGui(GuiGraphics guiGraphics, List<ItemIdentifierStack> allItems, @Nullable IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount) {
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, zLevel, displayAmount, true, false);
	}

	public static void renderItemIdentifierStackListIntoGui(GuiGraphics guiGraphics, List<ItemIdentifierStack> allItems, @Nullable IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount, boolean renderEffect, boolean ignoreDepth) {
		ItemStackRenderer itemStackRenderer = new ItemStackRenderer(0, 0, zLevel, renderEffect, ignoreDepth);
		itemStackRenderer.setDisplayAmount(displayAmount);
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, itemStackRenderer);
	}

	public static void renderItemIdentifierStackListIntoGui(GuiGraphics guiGraphics, List<ItemIdentifierStack> allItems, @Nullable IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, ItemStackRenderer itemStackRenderer) {
		int ppi = 0;
		int column = 0;
		int row = 0;

		for (ItemIdentifierStack identifierStack : allItems) {
			if (identifierStack == null) {
				column++;
				if (column >= columns) {
					row++;
					column = 0;
				}
				ppi++;
				continue;
			}
			ItemIdentifier item = identifierStack.getItem();
			if (IItemSearch != null && !IItemSearch.itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= items * page) {
				continue;
			}
			if (ppi > items * (page + 1)) {
				continue;
			}
			ItemStack itemstack = identifierStack.unsafeMakeNormalStack();
			int x = left + xSize * column;
			int y = top + ySize * row + 1;

			if (!itemstack.isEmpty()) {
				itemStackRenderer.setItemstack(itemstack).setPosX(x).setPosY(y);
				itemStackRenderer.renderInGui(guiGraphics);
			}

			column++;
			if (column >= columns) {
				row++;
				column = 0;
			}
		}
	}

	public void renderInGui(@Nullable GuiGraphics guiGraphics) {
		if (guiGraphics == null) {
            return;
        }

		ItemStack stack = itemstack;
		if (stack.isEmpty() && itemIdentStack != null) {
			stack = itemIdentStack.getItem().unsafeMakeNormalStack(1);
		}
		if (stack.isEmpty()) {
            return;
        }

        guiGraphics.renderItem(stack, posX, posY);

        if (displayAmount != DisplayAmount.NEVER) {
            long count = itemIdentStack != null ? itemIdentStack.getStackSize() : stack.getCount();
            String countLabel = TextUtil.getThreeDigitFormattedNumber(count, displayAmount == DisplayAmount.ALWAYS);
            guiGraphics.renderItemDecorations(font, stack, posX, posY, countLabel);
        }
        else {
            guiGraphics.renderItemDecorations(font, stack, posX, posY, null);
        }
	}

	private void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
		// no-op: replaced by GuiGraphics.renderItem in renderInGui()
	}

	public void renderInWorld() {
		// Legacy no-arg entry point — call sites that still use this path have no PoseStack
		// context and only run under the CCL-activated branch (currently dormant).
	}

	public void renderInWorld(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (itemstack.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		ItemRenderer ir = mc.getItemRenderer();
		ir.renderStatic(itemstack, net.minecraft.world.item.ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, bufferSource, mc.level, 0);
	}

	public void renderItemInGui(float x, float y, Item item, float zLevel, float scale) {
		GuiGraphics gg = SimpleGraphics.guiGraphics;
		if (gg == null || item == null) return;
		ItemStack stack = new ItemStack(item);
		if (stack.isEmpty()) return;
		PoseStack pose = gg.pose();
		pose.pushPose();
		pose.translate(x, y, zLevel);
		pose.scale(scale, scale, 1.0F);
		gg.renderItem(stack, 0, 0);
		pose.popPose();
	}

	public enum DisplayAmount {
		HIDE_ONE,
		ALWAYS,
		NEVER,
	}

}
