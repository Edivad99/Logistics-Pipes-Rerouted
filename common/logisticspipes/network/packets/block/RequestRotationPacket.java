package logisticspipes.network.packets.block;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class RequestRotationPacket extends CoordinatesPacket {

	public RequestRotationPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new RequestRotationPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		IRotationProvider tile = this.getTileOrPipe(player.level(), IRotationProvider.class);
		if (tile != null) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(Rotation.class).putInt(tile.getRotation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
		}
	}
}
