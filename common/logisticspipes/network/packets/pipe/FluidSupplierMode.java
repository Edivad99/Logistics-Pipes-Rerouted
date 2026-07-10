package logisticspipes.network.packets.pipe;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FluidSupplierMode extends IntegerCoordinatesPacket {

	public FluidSupplierMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new FluidSupplierMode(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (MainProxy.isClient(player.level())) {
			if (pipe.pipe instanceof PipeItemsFluidSupplier) {
				((PipeItemsFluidSupplier) pipe.pipe).setRequestingPartials((getInteger() % 10) == 1);
			}
			if (pipe.pipe instanceof PipeFluidSupplierMk2) {
				((PipeFluidSupplierMk2) pipe.pipe).setRequestingPartials((getInteger() % 10) == 1);
			}
		} else {
			if (pipe.pipe instanceof PipeItemsFluidSupplier) {
				PipeItemsFluidSupplier liquid = (PipeItemsFluidSupplier) pipe.pipe;
				liquid.setRequestingPartials((getInteger() % 10) == 1);
			}
			if (pipe.pipe instanceof PipeFluidSupplierMk2) {
				PipeFluidSupplierMk2 liquid = (PipeFluidSupplierMk2) pipe.pipe;
				liquid.setRequestingPartials((getInteger() % 10) == 1);
			}
		}
	}
}
