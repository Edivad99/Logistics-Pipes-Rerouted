package logisticspipes.network.packets.block;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class Rotation extends IntegerCoordinatesPacket {

	public Rotation(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new Rotation(getId());
	}

	@Override
	public void processPacket(Player player) {
		IRotationProvider tile = this.getTileOrPipe(player.level(), IRotationProvider.class);
		if (tile != null) {
			tile.setRotation(getInteger());
			// notifyNeighborsRespectDebug removed in 1.20.1 — use updateNeighborsAt
			BlockPos rPos = new BlockPos(getPosX(), getPosY(), getPosZ());
			player.level().updateNeighborsAt(rPos, player.level().getBlockState(rPos).getBlock());
		}
	}
}
