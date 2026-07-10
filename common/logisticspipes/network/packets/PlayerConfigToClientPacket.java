package logisticspipes.network.packets;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PlayerConfigToClientPacket extends ModernPacket {

	@Setter
	private ClientConfiguration config;

	public PlayerConfigToClientPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		LogisticsPipes.getClientPlayerConfig().read(input);
	}

	@Override
	public void processPacket(Player player) {}

	@Override
	public void writeData(LPDataOutput output) {
		config.write(output);
	}

	@Override
	public ModernPacket template() {
		return new PlayerConfigToClientPacket(getId());
	}
}
