package logisticspipes.network.packets;

import net.minecraft.world.entity.player.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.modplugins.nei.LoadingHelper;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ActivateNBTDebug extends ModernPacket {

	public ActivateNBTDebug(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		try {
			Class.forName("codechicken.nei.handler.NEIClientEventHandler");
			Configs.TOOLTIP_INFO = true;
			// LoadingHelper.LoadNeiNBTDebugHelper(); // NEI removed in 1.20.1
		} catch (ClassNotFoundException ignored) {

		} catch (Exception e1) {
			if (LogisticsPipes.isDEBUG()) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new ActivateNBTDebug(getId());
	}
}
