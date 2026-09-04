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
import logisticspipes.modules.SimpleFilter;
import logisticspipes.world.inventory.SimpleFilterMenu;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

public class SimpleFilterScreen extends ModuleBaseScreen<SimpleFilterMenu> {

    private static final Identifier TEXTURE = LPConstants.rl("textures/gui/itemsink.png");
    private final SimpleFilter filter;

    public SimpleFilterScreen(SimpleFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.getModule(), 175, 142);
        filter = (SimpleFilter) menu.getModule();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Asked of the interface, not of the concrete inventory: a module whose filter is a
        // property wraps one rather than being one, and the cast that used to be here threw.
        final String name = filter.getFilterInventory() instanceof IItemIdentifierInventory inventory
            ? inventory.getName() : "";
        guiGraphics.text(minecraft.font, name, 8, 6, 0xFF404040, false);
        guiGraphics.text(minecraft.font, "Inventory", 8, panelHeight - 92, 0xFF404040, false);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
        // texture: SimpleFilterScreen.TEXTURE
        int j = leftPos;
        int k = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SimpleFilterScreen.TEXTURE, j, k, 0.0f, 0.0f, panelWidth,
            panelHeight, 256, 256);
    }
}
