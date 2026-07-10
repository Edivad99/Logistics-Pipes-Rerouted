package logisticspipes.network.packets.hud;

import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class HUDStartBlockWatchingPacket extends CoordinatesPacket {

	public HUDStartBlockWatchingPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDStartBlockWatchingPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		IBlockWatchingHandler tile = this.getTileAs(player.level(), IBlockWatchingHandler.class);
		if (tile != null) {
			tile.playerStartWatching(player);
		}
	}
}
