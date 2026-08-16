package logisticspipes.network.packets.block;

import logisticspipes.LogisticsPipes;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PowerJunctionCheatPacket extends CoordinatesPacket {

	public PowerJunctionCheatPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PowerJunctionCheatPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (!LogisticsPipes.isDEBUG()) {
			return;
		}
		final LogisticsPowerJunctionBlockEntity tile = this.getTileAs(player.level(), LogisticsPowerJunctionBlockEntity.class);
		if (tile != null) {
			tile.addEnergy(100000);
		}
	}
}
