package logisticspipes.network.packets;

import net.minecraft.world.entity.player.Player;

import lombok.Setter;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.config.ClientConfiguration;

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
