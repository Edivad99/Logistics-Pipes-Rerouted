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
				player.sendSystemMessage(Component.literal("Debug enabled on Server"));
			} else {
				player.sendSystemMessage(Component.literal("Debug disabled on Server"));
			}
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugResponse(getId());
	}
}
