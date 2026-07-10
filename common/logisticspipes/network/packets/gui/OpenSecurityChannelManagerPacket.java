package logisticspipes.network.packets.gui;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.block.SecurityChannelManagerGui;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class OpenSecurityChannelManagerPacket extends CoordinatesPacket {

	public OpenSecurityChannelManagerPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity securityTile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		IChannelManager manager = SimpleServiceLocator.channelManagerProvider.getChannelManager(player.level());
		NewGuiHandler.getGui(SecurityChannelManagerGui.class).setChannelInformations(manager.getAllowedChannels(player)).setTilePos(securityTile).open(player);
	}

	@Override
	public ModernPacket template() {
		return new OpenSecurityChannelManagerPacket(getId());
	}
}
