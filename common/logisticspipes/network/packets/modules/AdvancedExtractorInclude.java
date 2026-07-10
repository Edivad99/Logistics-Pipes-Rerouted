package logisticspipes.network.packets.modules;

import logisticspipes.network.abstractpackets.BooleanModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;

@StaticResolve
public class AdvancedExtractorInclude extends BooleanModuleCoordinatesPacket {

	public AdvancedExtractorInclude(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new AdvancedExtractorInclude(getId());
	}

	@Override
	public void processPacket(Player player) {
		AsyncAdvancedExtractor receiver = this.getLogisticsModule(player, AsyncAdvancedExtractor.class);
		if (receiver == null) {
			return;
		}
		receiver.getItemsIncluded().setValue(isFlag());
	}
}
