package logisticspipes.network.packets.orderer;

import logisticspipes.interfaces.IOrderManagerContentReceiver;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class OrdererManagerContent extends InventoryModuleCoordinatesPacket {

	public OrdererManagerContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new OrdererManagerContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe tile = this.getPipe(player.level());
		if (tile == null) {
			return;
		}
		if (tile.pipe instanceof IOrderManagerContentReceiver) {
			((IOrderManagerContentReceiver) tile.pipe).setOrderManagerContent(getIdentList());
		}
	}
}
