package logisticspipes.network.packets.block;

import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PowerJunctionLevel extends IntegerCoordinatesPacket {

	public PowerJunctionLevel(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PowerJunctionLevel(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsPowerJunctionTileEntity tile = this.getTileAs(player.level(), LogisticsPowerJunctionTileEntity.class);
		if (tile != null) {
			tile.handlePowerPacket(getInteger());
		}
	}
}
