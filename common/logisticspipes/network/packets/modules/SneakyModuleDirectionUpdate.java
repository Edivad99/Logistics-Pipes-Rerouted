package logisticspipes.network.packets.modules;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.DirectionModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.module.SneakyDirection;

@StaticResolve
public class SneakyModuleDirectionUpdate extends DirectionModuleCoordinatesPacket {

	public SneakyModuleDirectionUpdate(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SneakyModuleDirectionUpdate(getId());
	}

	@Override
	public void processPacket(Player player) {
		SneakyDirection sneakyModule = this.getLogisticsModule(player, SneakyDirection.class);
		if (sneakyModule == null) {
			return;
		}
		sneakyModule.setSneakyDirection(getDirection());
	}
}
