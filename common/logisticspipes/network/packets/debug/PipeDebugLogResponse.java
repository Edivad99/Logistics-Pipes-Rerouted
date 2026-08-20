package logisticspipes.network.packets.debug;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PipeDebugLogResponse extends CoordinatesPacket {

	public PipeDebugLogResponse(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe tile = this.getPipe(player.level());
		if (tile != null) {
			((CoreRoutedPipe) tile.pipe).debug.openForPlayer(player);
			player.displayClientMessage(Component.literal("Debug log enabled."), false);
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugLogResponse(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
