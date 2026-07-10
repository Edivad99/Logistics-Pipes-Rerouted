package logisticspipes.network.packets.pipe;

import javax.annotation.Nullable;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class ChassisOrientationPacket extends CoordinatesPacket {

	@Getter
	@Setter
	@Nullable
	private Direction dir;

	public ChassisOrientationPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			((PipeLogisticsChassis) pipe.pipe).setPointedOrientation(dir);
		}
	}

	@Override
	public ModernPacket template() {
		return new ChassisOrientationPacket(getId());
	}
}
