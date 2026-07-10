package logisticspipes.network.packets.module;

import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class OreDictItemSinkList extends NBTModuleCoordinatesPacket {

	public OreDictItemSinkList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new OreDictItemSinkList(getId());
	}

	@Override
	public void processPacket(Player player) {
		ModuleOreDictItemSink module = this.getLogisticsModule(player, ModuleOreDictItemSink.class);
		if (module == null) {
			return;
		}
		module.readFromNBT(getTag(), player.level().registryAccess());
		if (MainProxy.isServer(player.level()) && getType().isInWorld()) {
			module.OreListChanged();
		}
	}
}
