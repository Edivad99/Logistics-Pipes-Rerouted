package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.abstractpackets.Integer2ModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class FluidCraftingAmount extends Integer2ModuleCoordinatesPacket {

	public FluidCraftingAmount(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new FluidCraftingAmount(getId());
	}

	@Override
	public void processPacket(Player player) {
		ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		// integer2 is the slot and integer is the amount
		if (MainProxy.isClient(player.level())) {
			module.liquidAmounts.set(getInteger2(), getInteger());
		} else {
			module.changeFluidAmount(getInteger(), getInteger2(), player);
		}
	}
}
