package logisticspipes.network.packets.modules;

import logisticspipes.network.abstractpackets.IntegerModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class QuickSortState extends IntegerModuleCoordinatesPacket {

	public QuickSortState(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		QuickSortChestMarkerStorage.getInstance().setSlots(getPosX(), getPosY(), getPosZ(), getPositionInt(), getInteger());
	}

	@Override
	public ModernPacket template() {
		return new QuickSortState(getId());
	}
}
