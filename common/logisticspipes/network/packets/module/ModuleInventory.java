package logisticspipes.network.packets.module;

import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class ModuleInventory extends InventoryModuleCoordinatesPacket {

	public ModuleInventory(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ModuleInventory(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (getIdentList() == null) return;
		IModuleInventoryReceive module = this.getLogisticsModule(player, IModuleInventoryReceive.class);
		if (module == null) {
			return;
		}
		module.handleInvContent(getIdentList());
	}
}
