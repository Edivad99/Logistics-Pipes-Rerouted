package logisticspipes.network.packets.pipe;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class RequestSignPacket extends CoordinatesPacket {

	public RequestSignPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		((CoreRoutedPipe) pipe.pipe).sendSignData(player, false);
	}

	@Override
	public ModernPacket template() {
		return new RequestSignPacket(getId());
	}
}
