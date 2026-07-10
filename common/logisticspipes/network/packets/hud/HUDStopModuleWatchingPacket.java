package logisticspipes.network.packets.hud;

import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class HUDStopModuleWatchingPacket extends ModuleCoordinatesPacket {

	public HUDStopModuleWatchingPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDStopModuleWatchingPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		IModuleWatchReciver handler = this.getLogisticsModule(player, IModuleWatchReciver.class);
		if (handler == null) {
			return;
		}
		handler.stopWatching(player);
	}
}
