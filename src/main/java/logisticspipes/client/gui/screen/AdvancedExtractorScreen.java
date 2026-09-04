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
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.module.OpenSneakyDirectionGuiMessage;
import logisticspipes.network.to_server.module.SetModulePropertiesMessage;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.world.inventory.AdvancedExtractorMenu;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;

public class AdvancedExtractorScreen extends ModuleBaseScreen<AdvancedExtractorMenu> {

    private final AsyncAdvancedExtractor advancedExtractor;
    private final PropertyLayer propertyLayer;
    private final ValuePropertyOverlay<Boolean, BooleanProperty> itemsIncludedOverlay;

    public AdvancedExtractorScreen(AdvancedExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.getModule(), 175, 142);
        this.advancedExtractor = menu.getExtractor();
        propertyLayer = new PropertyLayer(this.advancedExtractor.getProperties());
        itemsIncludedOverlay = propertyLayer.overlay(this.advancedExtractor.getItemsIncluded());
    }

    @Override
    public void init() {
        super.init();
        //Default item toggle:
        addRenderableWidget(new GuiStringHandlerButton(0, width / 2 + 20, height / 2 - 34, 60, 20,
            () -> itemsIncludedOverlay.get() ? "Included" : "Excluded",
            () -> itemsIncludedOverlay.write(BooleanProperty::toggle)));

        logisticspipes.utils.gui.SmallGuiButton sneaky = new logisticspipes.utils.gui.SmallGuiButton(1, width / 2 - 25,
            height / 2 - 34, 40, 20, "Sneaky");
        sneaky.setPressListener(b -> ClientPacketDistributor.sendToServer(
            new OpenSneakyDirectionGuiMessage(ModuleTarget.of(advancedExtractor))));
        addRenderableWidget(sneaky);
    }

    @Override
    public void onClose() {
        super.onClose();
        propertyLayer.unregister();
        if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
            // send update to server, when there are changed properties
            ClientPacketDistributor.sendToServer(SetModulePropertiesMessage.of(
                ModuleTarget.of(module), propertyLayer, this.minecraft.level.registryAccess()));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(minecraft.font, advancedExtractor.getFilterInventory().getName(), 8, 6, 0xFF404040, false);
        guiGraphics.text(minecraft.font, "Inventory", 8, panelHeight - 92, 0xFF404040, false);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
        // texture: LogisticsBaseGuiScreen.ITEMSINK
        int j = leftPos;
        int k = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LogisticsBaseGuiScreen.ITEMSINK, j, k, 0.0f, 0.0f, panelWidth,
            panelHeight, 256, 256);
    }

}
