package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class SecurityStationOpenPlayerRequest extends StringCoordinatesPacket {

	public SecurityStationOpenPlayerRequest(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationOpenPlayerRequest(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			if (getString() != null && !getString().isEmpty()) {
				tile.handleOpenSecurityPlayer(player, getString());
			}
		}
	}
}
