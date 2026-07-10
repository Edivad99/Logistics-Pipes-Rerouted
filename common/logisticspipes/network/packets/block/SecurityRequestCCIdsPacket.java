package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class SecurityRequestCCIdsPacket extends CoordinatesPacket {

	public SecurityRequestCCIdsPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityRequestCCIdsPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.requestList(player);
		}
	}
}
