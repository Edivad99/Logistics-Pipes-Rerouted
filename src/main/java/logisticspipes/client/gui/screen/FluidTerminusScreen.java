package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.pipe.SetPipePropertiesMessage;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.inventory.FluidTerminusMenu;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;

public class FluidTerminusScreen extends LogisticsBaseGuiScreen<FluidTerminusMenu> {

    private final PropertyLayer propertyLayer;
    private final BlockPos pipePosition;
    private final PropertyOverlay<ItemIdentifierInventory, ItemIdentifierInventoryProperty> sinkInventoryOverlay;

    public FluidTerminusScreen(FluidTerminusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 180, 130, 0, 0);
        final PipeFluidTerminus pipe = menu.getPipe();

        pipePosition = pipe.getPos();
        propertyLayer = new PropertyLayer(pipe.getProperties());
        sinkInventoryOverlay = propertyLayer.overlay(pipe.getSinkInv());

    }

    @Override
    public void onClose() {
        super.onClose();
        propertyLayer.unregister();
        if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
            // send update to server, when there are changed properties
            ClientPacketDistributor.sendToServer(SetPipePropertiesMessage.of(
                pipePosition, propertyLayer, this.minecraft.level.registryAccess()));
        }
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 45);
        for (int i = 0; i < 9; i++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9 + i * 18, topPos + 18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font, "Fluid Terminus", 10, 8, 0xFF404040, false);
    }
}
