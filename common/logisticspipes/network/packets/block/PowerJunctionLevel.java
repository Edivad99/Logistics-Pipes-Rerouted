package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

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
		LogisticsPowerJunctionBlockEntity tile = this.getTileAs(player.level(), LogisticsPowerJunctionBlockEntity.class);
		if (tile != null) {
			tile.handlePowerPacket(getInteger());
		}
	}
}
