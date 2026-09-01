package logisticspipes.network.guis.pipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.gui.GuiFluidSupplierPipe;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class FluidSupplierGui extends CoordinatesGuiProvider {

	public FluidSupplierGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeItemsFluidSupplier)) return null;
		PipeItemsFluidSupplier pipe = (PipeItemsFluidSupplier) tile.pipe;
		return new GuiFluidSupplierPipe(player.getInventory(), pipe.getDummyInventory(), pipe);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeItemsFluidSupplier)) return null;
		PipeItemsFluidSupplier pipe = (PipeItemsFluidSupplier) tile.pipe;
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.getDummyInventory());
		dummy.addNormalSlotsForPlayerInventory(18, 97);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				dummy.addDummySlot(column + row * 3, 72 + column * 18, 18 + row * 18);
			}
		}
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer,
					new FluidSupplierPartialsMessage(tile.getBlockPos(), pipe.isRequestingPartials()));
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidSupplierGui(getId());
	}
}
