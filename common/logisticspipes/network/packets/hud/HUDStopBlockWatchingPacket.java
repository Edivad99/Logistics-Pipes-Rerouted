package logisticspipes.network.packets.hud;

import net.minecraft.world.entity.player.Player;

import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class HUDStopBlockWatchingPacket extends CoordinatesPacket {

	public HUDStopBlockWatchingPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDStopBlockWatchingPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		IBlockWatchingHandler tile = this.getTileAs(player.level(), IBlockWatchingHandler.class);
		if (tile != null) {
			tile.playerStopWatching(player);
		}
	}
}
