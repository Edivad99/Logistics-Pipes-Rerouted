package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.client.gui.popup.GuiDiskPopup;
import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.network.to_server.orderer.DropDiskMessage;
import logisticspipes.network.to_server.orderer.RequestDiskContentMessage;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.OrdererMk2Menu;

public class NormalMk2OrdererScreen extends NormalOrdererScreen<OrdererMk2Menu> implements IDiskProvider {

    public final PipeItemsRequestLogisticsMk2 pipe;
    private SmallGuiButton macroButton;

    public NormalMk2OrdererScreen(OrdererMk2Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        pipe = menu.getPipe();
        ClientPacketDistributor.sendToServer(new RequestDiskContentMessage(pipe.getPos()));
    }

    @Override
    public void init() {
        super.init();
        macroButton = new SmallGuiButton(12, right - 55, bottom - 60, 50, 10, "Disk");
        macroButton.setPressListener(b -> {
            ClientPacketDistributor.sendToServer(new RequestDiskContentMessage(pipe.getPos()));
            setSubGui(new GuiDiskPopup(this));
        });
        addRenderableWidget(macroButton);
        macroButton.active = false;
    }

    @Override
    public void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        super.extractGuiBackground(guiGraphics, i, j, f);

        guiGraphics.fill(right - 39, bottom - 47, right - 19, bottom - 27, Color.getValue(Color.BLACK));
        guiGraphics.fill(right - 37, bottom - 45, right - 21, bottom - 29, Color.getValue(Color.DARKER_GREY));

        if (!pipe.getDisk().isEmpty()) {
            guiGraphics.item(pipe.getDisk(), right - 36, bottom - 44);
            macroButton.active = true;
        } else {
            macroButton.active = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        int k = event.button();
        if (x >= right - 39 && x < right - 19 && y >= bottom - 47 && y < bottom - 27) {
            ClientPacketDistributor.sendToServer(new DropDiskMessage(pipe.getPos()));
            return true;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
    }

    @Override
    public ItemStack getDisk() {
        return pipe.getDisk();
    }

    @Override
    public void specialItemRendering(ItemIdentifier item, int x, int y) {
        // ThaumCraft integration not ported to 1.20.1
    }

    @Override
    public BlockPos getBlockPos() {
        return pipe.getPos();
    }

    @Override
    public ItemDisplay getItemDisplay() {
        return itemDisplay;
    }
}
