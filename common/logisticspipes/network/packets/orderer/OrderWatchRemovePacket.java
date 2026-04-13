package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OrderWatchRemovePacket extends IntegerCoordinatesPacket {

	public OrderWatchRemovePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe tile = this.getPipe(player.level());
		if (tile != null && tile.pipe instanceof IRequestWatcher) {
			((IRequestWatcher) tile.pipe).handleClientSideRemove(getInteger());
		}
	}

	@Override
	public ModernPacket template() {
		return new OrderWatchRemovePacket(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
