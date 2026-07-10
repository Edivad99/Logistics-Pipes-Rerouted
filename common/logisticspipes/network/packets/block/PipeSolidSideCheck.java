package logisticspipes.network.packets.block;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class PipeSolidSideCheck extends IntegerCoordinatesPacket {

	public PipeSolidSideCheck(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PipeSolidSideCheck(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		pipe.renderState.checkForRenderUpdate(player.level(), pipe.getBlockPos());
	}
}
