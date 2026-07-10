package logisticspipes.network.packets.pipe;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidSupplierMk2.MinMode;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FluidSupplierMinMode extends IntegerCoordinatesPacket {

	public FluidSupplierMinMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new FluidSupplierMinMode(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeFluidSupplierMk2) {
			((PipeFluidSupplierMk2) pipe.pipe).setMinMode(MinMode.values()[getInteger()]);
		}
	}
}
