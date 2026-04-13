package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityAuthorizationPacket extends IntegerCoordinatesPacket {

	public SecurityAuthorizationPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityAuthorizationPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			if (getInteger() == 1) {
				tile.authorizeStation();
			} else {
				tile.deauthorizeStation();
			}
		}
	}
}
