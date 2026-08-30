package logisticspipes.network.packets.chassis;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class EnableQuickSortMarker extends ModernPacket {

	public EnableQuickSortMarker(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		QuickSortChestMarkerStorage.getInstance().enable();
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new EnableQuickSortMarker(getId());
	}
}
