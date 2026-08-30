package logisticspipes.network.packets;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ActivateNBTDebug extends ModernPacket {

	public ActivateNBTDebug(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		// NEI does not exist on 1.20.1 — the former Class.forName("codechicken.nei...") check
		// always failed, making this packet a no-op.
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new ActivateNBTDebug(getId());
	}
}
