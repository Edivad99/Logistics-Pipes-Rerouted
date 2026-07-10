package logisticspipes.network.packets.pipe;

import logisticspipes.network.abstractpackets.BitSetCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FireWallFlag extends BitSetCoordinatesPacket {

	public FireWallFlag(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new FireWallFlag(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsFirewall) {
			PipeItemsFirewall firewall = (PipeItemsFirewall) pipe.pipe;
			firewall.setFlags(getFlags());
		}
	}
}
