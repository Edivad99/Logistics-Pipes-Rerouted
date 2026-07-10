package logisticspipes.network.guis.pipe;

import logisticspipes.gui.GuiFluidSupplierPipe;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.packets.pipe.FluidSupplierMode;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidSupplierMode.class)
				.putInt(pipe.isRequestingPartials() ? 1 : 0).setBlockPos(tile.getBlockPos()), player);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidSupplierGui(getId());
	}
}
