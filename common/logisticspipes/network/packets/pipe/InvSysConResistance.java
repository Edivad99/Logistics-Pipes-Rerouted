package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class InvSysConResistance extends IntegerCoordinatesPacket {

	public InvSysConResistance(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new InvSysConResistance(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (MainProxy.isClient(player.level())) {
			final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
			if (pipe == null) {
				return;
			}
			if (pipe.pipe instanceof PipeItemsInvSysConnector) {
				PipeItemsInvSysConnector invCon = (PipeItemsInvSysConnector) pipe.pipe;
				invCon.resistance = getInteger();
			}
		} else {
			final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
			if (pipe == null) {
				return;
			}
			if (pipe.pipe instanceof PipeItemsInvSysConnector) {
				PipeItemsInvSysConnector invCon = (PipeItemsInvSysConnector) pipe.pipe;
				invCon.resistance = getInteger();
				invCon.getRouter().update(true, invCon);
			}
		}
	}
}
