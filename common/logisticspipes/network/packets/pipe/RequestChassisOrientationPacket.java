package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestChassisOrientationPacket extends CoordinatesPacket {

	public RequestChassisOrientationPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ChassisOrientationPacket.class).setDir(((PipeLogisticsChassis) pipe.pipe).getPointedOrientation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
		}
	}

	@Override
	public ModernPacket template() {
		return new RequestChassisOrientationPacket(getId());
	}
}
