package logisticspipes.network.packets.cpipe;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class CPipeCleanupImport extends ModuleCoordinatesPacket {

	public CPipeCleanupImport(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new CPipeCleanupImport(getId());
	}

	@Override
	public void processPacket(Player player) {
		final ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.importCleanup();
	}
}
