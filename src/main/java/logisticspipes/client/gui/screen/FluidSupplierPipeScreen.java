/**
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

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.world.inventory.FluidSupplierMenu;
import network.rs485.logisticspipes.util.TextUtil;

public class FluidSupplierPipeScreen extends LogisticsBaseGuiScreen<FluidSupplierMenu> {

    protected static final Identifier SUPPLIER = LPConstants.rl("textures/gui/supplier.png");
    private static final String PREFIX = "gui.fluidsupplier.";
    private final PipeItemsFluidSupplier logic;

    public FluidSupplierPipeScreen(FluidSupplierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 186, 0, 0);
        this.logic = menu.getPipe();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(minecraft.font, TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "TargetInv"),
            panelWidth / 2 - minecraft.font.width(TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "TargetInv")) / 2,
            6, 0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "Inventory"), 18,
            panelHeight - 102, 0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "Partialrequests") + ":",
            panelWidth - 140, panelHeight - 112, 0xFF404040, false);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
        // texture: FluidSupplierPipeScreen.SUPPLIER
        int j = leftPos;
        int k = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FluidSupplierPipeScreen.SUPPLIER, j, k, 0.0f, 0.0f, panelWidth,
            panelHeight, 256, 256);
    }

    @Override
    public void init() {
        super.init();
        logisticspipes.utils.gui.SmallGuiButton partialsBtn = new logisticspipes.utils.gui.SmallGuiButton(0,
            width / 2 + 45, height / 2 - 25, 30, 20, logic.isRequestingPartials() ?
            TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "Yes") :
            TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "No"));
        partialsBtn.setPressListener(b -> {
            logic.setRequestingPartials(!logic.isRequestingPartials());
            b.setMessage(Component.literal(logic.isRequestingPartials() ?
                TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "Yes") :
                TextUtil.translate(FluidSupplierPipeScreen.PREFIX + "No")));
            ClientPacketDistributor.sendToServer(
                new FluidSupplierPartialsMessage(logic.getPos(), logic.isRequestingPartials()));
        });
        addRenderableWidget(partialsBtn);
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
