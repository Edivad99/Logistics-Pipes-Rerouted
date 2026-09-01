package logisticspipes.network.guis.pipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.gui.GuiFluidSupplierMk2Pipe;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.bidirectional.FluidSupplierMinModeMessage;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

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
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer,
					new FluidSupplierPartialsMessage(tile.getBlockPos(), pipe.isRequestingPartials()));
			PacketDistributor.sendToPlayer(serverPlayer,
					new FluidSupplierMinModeMessage(tile.getBlockPos(), pipe.getMinMode()));
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidSupplierMk2Gui(getId());
	}
}
