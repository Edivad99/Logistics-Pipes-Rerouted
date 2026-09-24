/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.Translations;
import logisticspipes.network.bidirectional.FluidSupplierMinModeMessage;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.network.to_server.pipe.ChangeFluidSupplierAmountMessage;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidSupplierMk2.MinMode;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.world.inventory.FluidSupplierMk2Menu;

public class FluidSupplierMk2PipeScreen extends LogisticsBaseGuiScreen<FluidSupplierMk2Menu> {

    private final PipeFluidSupplierMk2 logic;

    public FluidSupplierMk2PipeScreen(FluidSupplierMk2Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 184, 176, 0, 0);
        this.logic = menu.getPipe();
        ClientPacketDistributor.sendToServer(new ChangeFluidSupplierAmountMessage(this.logic.getPos(), 0));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(minecraft.font, Component.translatable(Translations.Screen.FLUID_SUPPLIER_MK2_TARGET),
            panelWidth / 2
                - minecraft.font.width(Component.translatable(Translations.Screen.FLUID_SUPPLIER_MK2_TARGET)) / 2, 6,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, Component.translatable(Translations.Screen.INVENTORY), 15,
            panelHeight - 95, 0xFF404040, false);
        guiGraphics.text(minecraft.font,
            Component.translatable(Translations.Screen.FLUID_SUPPLIER_MK2_FLUID).append(":"),
            25, 22, 0xFF404040, false);
        guiGraphics.text(minecraft.font,
            Component.translatable(Translations.Screen.FLUID_SUPPLIER_MK2_PARTIAL).append(":"),
            panelWidth - 176, panelHeight - 109, 0xFF404040, false);
        guiGraphics.text(minecraft.font,
            Component.translatable(Translations.Screen.FLUID_SUPPLIER_MK2_MIN_MODE).append(":"),
            panelWidth - 108, panelHeight - 109, 0xFF404040, false);
        guiGraphics.text(minecraft.font, Integer.toString(logic.getAmount()), panelWidth / 2, 22, 0xFF404040, false);
        guiGraphics.text(minecraft.font, "+", 32, 39, 0xFF404040, false);
        guiGraphics.text(minecraft.font, "-", 32, 50, 0xFF404040, false);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0.0f,
            true);
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 13, topPos + 92);
        LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 59, topPos + 17);
        //RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        //minecraft.renderEngine.func_110577_a("/logisticspipes/gui/supplier.png");
        //int j = leftPos;
        //int k = topPos;
        //drawTexturedModalRect(j, k, 0, 0, panelWidth, panelHeight);
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton partialsBtn = new SmallGuiButton(0, width / 2 - 48, topPos + panelHeight - 115, 30, 20,
            Component.translatable(logic.isRequestingPartials() ? Translations.Screen.YES : Translations.Screen.NO));
        partialsBtn.setPressListener(b -> {
            logic.setRequestingPartials(!logic.isRequestingPartials());
            b.setMessage(Component.translatable(logic.isRequestingPartials() ?
                Translations.Screen.YES :
                Translations.Screen.NO));
            ClientPacketDistributor.sendToServer(
                new FluidSupplierPartialsMessage(logic.getPos(), logic.isRequestingPartials()));
        });
        addRenderableWidget(partialsBtn);
        SmallGuiButton minModeBtn = new SmallGuiButton(1, width / 2 + 30, topPos + panelHeight - 115, 55, 20,
            minModeLabel());
        minModeBtn.setPressListener(b -> {
            int index = logic.getMinMode().ordinal() + 1;
            if (index >= MinMode.values().length) {
                index = 0;
            }
            logic.setMinMode(MinMode.values()[index]);
            b.setMessage(minModeLabel());
            ClientPacketDistributor.sendToServer(new FluidSupplierMinModeMessage(logic.getPos(), logic.getMinMode()));
        });
        addRenderableWidget(minModeBtn);
        int[] amounts = { 1, 10, 100, 1000 };
        int[] ys = { topPos + 37, topPos + 48 };
        int[] xs = { leftPos + 40, leftPos + 51, leftPos + 72, leftPos + 103 };
        int[] widths = { 10, 20, 30, 40 };
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 2; row++) {
                final int change = (row == 0 ? 1 : -1) * amounts[col];
                SmallGuiButton amtBtn = new SmallGuiButton((col + 1) * 10 + row, xs[col], ys[row], widths[col], 10,
                    Integer.toString(amounts[col]));
                amtBtn.setPressListener(b -> ClientPacketDistributor.sendToServer(
                    new ChangeFluidSupplierAmountMessage(logic.getPos(), change)));
                addRenderableWidget(amtBtn);
            }
        }
    }

    private Component minModeLabel() {
        return Component.translatable(
            Translations.Screen.enumValue("fluid_supplier_mk2.min_amount", logic.getMinMode()));
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
