package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class SecurityRemoveCCIdPacket extends IntegerCoordinatesPacket {

	public SecurityRemoveCCIdPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityRemoveCCIdPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.removeCCFromList(getInteger());
			tile.requestList(player);
		}
	}
}
