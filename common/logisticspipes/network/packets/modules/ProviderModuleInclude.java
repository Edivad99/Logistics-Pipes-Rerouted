package logisticspipes.network.packets.modules;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.abstractpackets.BooleanModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class ProviderModuleInclude extends BooleanModuleCoordinatesPacket {

	public ProviderModuleInclude(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ProviderModuleInclude(getId());
	}

	@Override
	public void processPacket(Player player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.isExclusionFilter.setValue(isFlag());
	}
}
