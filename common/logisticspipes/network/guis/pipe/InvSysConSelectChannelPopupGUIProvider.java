package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.gui.popup.GuiSelectChannelPopup;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractpackets.ChannelInformationListCoordinatesPopupGuiProvider;
import logisticspipes.network.to_server.SetInvSysConChannelMessage;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class InvSysConSelectChannelPopupGUIProvider extends ChannelInformationListCoordinatesPopupGuiProvider {

	public InvSysConSelectChannelPopupGUIProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe bPipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(bPipe.pipe instanceof PipeItemsInvSysConnector)) {
			return null;
		}

		return new GuiSelectChannelPopup(getChannelInformations(), bPipe.getBlockPos(), sel -> {
			ClientPacketDistributor.sendToServer(
					new SetInvSysConChannelMessage(bPipe.getBlockPos(), sel.getChannelIdentifier()));
		});
	}

	@Override
	public GuiProvider template() {
		return new InvSysConSelectChannelPopupGUIProvider(getId());
	}
}
