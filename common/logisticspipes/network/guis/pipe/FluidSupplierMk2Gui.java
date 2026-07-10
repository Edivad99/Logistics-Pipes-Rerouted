package logisticspipes.network.guis.pipe;

import logisticspipes.gui.GuiFluidSupplierMk2Pipe;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.packets.pipe.FluidSupplierMinMode;
import logisticspipes.network.packets.pipe.FluidSupplierMode;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FluidSupplierMk2Gui extends CoordinatesGuiProvider {

	public FluidSupplierMk2Gui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeFluidSupplierMk2)) return null;
		PipeFluidSupplierMk2 pipe = (PipeFluidSupplierMk2) tile.pipe;
		return new GuiFluidSupplierMk2Pipe(player.getInventory(), pipe.getDummyInventory(), pipe);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeFluidSupplierMk2)) return null;
		PipeFluidSupplierMk2 pipe = (PipeFluidSupplierMk2) tile.pipe;
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.getDummyInventory());
		dummy.addNormalSlotsForPlayerInventory(18, 97);
		dummy.addFluidSlot(0, pipe.getDummyInventory(), 0, 0);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidSupplierMode.class)
				.putInt(pipe.isRequestingPartials() ? 1 : 0).setBlockPos(tile.getBlockPos()), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidSupplierMinMode.class)
				.putInt(pipe.getMinMode().ordinal()).setBlockPos(tile.getBlockPos()), player);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidSupplierMk2Gui(getId());
	}
}
