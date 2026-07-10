package logisticspipes.network.packets;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class BufferTransfer extends ModernPacket {

	@Getter
	@Setter
	private byte[] content;

	public BufferTransfer(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new BufferTransfer(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (MainProxy.isClient(player.level())) {
			SimpleServiceLocator.clientBufferHandler.handlePacket(content);
		} else {
			SimpleServiceLocator.serverBufferHandler.handlePacket(content, player);
		}
	}

	@Override
	public void readData(LPDataInput input) {
		content = input.readByteArray();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeByteArray(content);
	}
}
