package logisticspipes.network.packets;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

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
