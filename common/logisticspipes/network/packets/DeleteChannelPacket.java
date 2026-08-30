package logisticspipes.network.packets;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DeleteChannelPacket extends ModernPacket {

	@Getter
	@Setter
	private UUID channelIdentifier;

	public DeleteChannelPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		channelIdentifier = input.readUUID();
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeUUID(channelIdentifier);
	}

	@Override
	public void processPacket(Player player) {
		IChannelManager manager = SimpleServiceLocator.channelManagerProvider.getChannelManager(player.level());
		manager.removeChannel(channelIdentifier);
	}

	@Override
	public ModernPacket template() {
		return new DeleteChannelPacket(getId());
	}
}
