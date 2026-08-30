package logisticspipes.network.packets;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DummyPacket extends ModernPacket {

	public DummyPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		throw new RuntimeException("This packet should never be used");
	}

	@Override
	public void processPacket(Player player) {
		throw new RuntimeException("This packet should never be used");
	}

	@Override
	public void writeData(LPDataOutput output) {
		throw new RuntimeException("This packet should never be used");
	}

	@Override
	public ModernPacket template() {
		return new DummyPacket(getId());
	}
}
