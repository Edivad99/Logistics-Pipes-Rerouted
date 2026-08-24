package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class FluidSupplierAmount extends IntegerCoordinatesPacket {

	public FluidSupplierAmount(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new FluidSupplierAmount(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (!(pipe.pipe instanceof PipeFluidSupplierMk2)) {
			return;
		}
		if (MainProxy.isClient(player.level())) {
			((PipeFluidSupplierMk2) pipe.pipe).setAmount(getInteger());
		} else {
			((PipeFluidSupplierMk2) pipe.pipe).changeFluidAmount(getInteger(), player);
		}
	}
}
