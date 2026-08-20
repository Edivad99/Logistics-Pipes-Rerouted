package logisticspipes.network.packets.pipe;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PipeDebugResponse extends CoordinatesPacket {

	public PipeDebugResponse(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe != null && pipe.isInitialized()) {
			pipe.pipe.debug.debugThisPipe = !pipe.pipe.debug.debugThisPipe;
			if (pipe.pipe.debug.debugThisPipe) {
				player.displayClientMessage(Component.literal("Debug enabled on Server"), false);
			} else {
				player.displayClientMessage(Component.literal("Debug disabled on Server"), false);
			}
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugResponse(getId());
	}
}
