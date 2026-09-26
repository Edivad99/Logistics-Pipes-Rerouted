package logisticspipes.client.gui.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.Translations;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.network.to_server.orderer.RequestOrdererRefreshMessage;
import logisticspipes.request.RequestHandler.DisplayOptions;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.OrdererMenu;

public class NormalOrdererScreen<T extends OrdererMenu> extends OrdererScreen<T> {

    private DisplayOptions displayOptions = DisplayOptions.Both;

    public NormalOrdererScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        refreshItems();
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton refreshBtn = new SmallGuiButton(3, leftPos + 10, bottom - 15, 46, 10,
            Component.translatable(Translations.Screen.REFRESH));
        refreshBtn.setPressListener(b -> refreshItems());
        addRenderableWidget(refreshBtn);
        addRenderableWidget(new SmallGuiButton(13, leftPos + 10, bottom - 28, 46, 10,
            Component.translatable(Translations.Screen.ORDERER_CONTENT)));
        SmallGuiButton modeBtn = new SmallGuiButton(9, leftPos + 10, bottom - 41, 46, 10, displayOptionsLabel());
        modeBtn.setPressListener(b -> {
            displayOptions = switch (displayOptions) {
                case Both -> DisplayOptions.CraftOnly;
                case CraftOnly -> DisplayOptions.SupplyOnly;
                case SupplyOnly -> DisplayOptions.Both;
            };
            b.setMessage(displayOptionsLabel());
            refreshItems();
        });
        addRenderableWidget(modeBtn);
    }

    private Component displayOptionsLabel() {
        return Component.translatable(Translations.Screen.enumValue("orderer.display", displayOptions));
    }

    @Override
    public void refreshItems() {
        ClientPacketDistributor.sendToServer(new RequestOrdererRefreshMessage(
            new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord)), displayOptions));
    }

    @Override
    public void specialItemRendering(ItemIdentifier item, int x, int y) {
    }
}
