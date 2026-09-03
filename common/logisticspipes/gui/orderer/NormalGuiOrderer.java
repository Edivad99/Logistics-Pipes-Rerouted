package logisticspipes.gui.orderer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.RemotePipeTarget;
import logisticspipes.network.to_server.orderer.RequestOrdererRefreshMessage;
import logisticspipes.request.RequestHandler.DisplayOptions;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.OrdererMenu;

public class NormalGuiOrderer extends GuiOrderer {

	private DisplayOptions displayOptions = DisplayOptions.Both;

	public NormalGuiOrderer(OrdererMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		refreshItems();
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton refreshBtn = new SmallGuiButton(3, leftPos + 10, bottom - 15, 46, 10, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		addRenderableWidget(new SmallGuiButton(13, leftPos + 10, bottom - 28, 46, 10, "Content"));
		SmallGuiButton modeBtn = new SmallGuiButton(9, leftPos + 10, bottom - 41, 46, 10, "Both");
		modeBtn.setPressListener(b -> {
			String displayString = switch (displayOptions) {
                case Both -> {
                    displayOptions = DisplayOptions.CraftOnly;
                    yield "Craft";
                }
                case CraftOnly -> {
                    displayOptions = DisplayOptions.SupplyOnly;
                    yield "Supply";
                }
                case SupplyOnly -> {
                    displayOptions = DisplayOptions.Both;
                    yield "Both";
                }
            };
            b.setMessage(Component.literal(displayString));
			refreshItems();
		});
		addRenderableWidget(modeBtn);
	}

	@Override
	public void refreshItems() {
        ClientPacketDistributor.sendToServer(new RequestOrdererRefreshMessage(
                new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord)), displayOptions));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
