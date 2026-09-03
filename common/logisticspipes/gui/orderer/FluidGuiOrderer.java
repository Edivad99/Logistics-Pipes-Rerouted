package logisticspipes.gui.orderer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.RemotePipeTarget;
import logisticspipes.network.to_server.orderer.RequestFluidOrdererRefreshMessage;
import logisticspipes.network.to_server.orderer.SubmitFluidRequestMessage;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.FluidOrdererMenu;

public class FluidGuiOrderer extends GuiOrderer {

	public FluidGuiOrderer(FluidOrdererMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.title = "Request Fluid";
		refreshItems();
	}

	@Override
	public void init() {
		boolean setItemDisplay = itemDisplay == null;
		super.init();
		SmallGuiButton refreshBtn = new SmallGuiButton(3, leftPos + 10, bottom - 25, 46, 20, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		if (setItemDisplay) {
			itemDisplay = new ItemDisplay(this, font, this, this, leftPos + 10, topPos + 18, panelWidth - 20, panelHeight - 100, xCenter, bottom - 24, 49, new int[] { 1, 1000, 16000, 100 }, false);
		}
		itemDisplay.reposition(leftPos + 10, topPos + 18, panelWidth - 20, panelHeight - 100, xCenter, bottom - 24);
	}

	@Override
	protected int getStackAmount() {
		return 1000;
	}

	@Override
	protected void submitRequest() {
		ClientPacketDistributor.sendToServer(new SubmitFluidRequestMessage(
				new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord)),
				itemDisplay.getSelectedItem().getItem().makeStack(itemDisplay.getRequestCount())));
	}

	@Override
	public void refreshItems() {
		ClientPacketDistributor.sendToServer(new RequestFluidOrdererRefreshMessage(
				new RemotePipeTarget(dimension, new BlockPos(xCoord, yCoord, zCoord))));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
