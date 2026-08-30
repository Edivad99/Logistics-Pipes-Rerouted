package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.GuiFluidBasic;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.fluid.FluidSinkPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class FluidBasicGui extends CoordinatesGuiProvider {

	public FluidBasicGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof FluidSinkPipe)) return null;
		FluidSinkPipe pipe = (FluidSinkPipe) tile.pipe;
		return new GuiFluidBasic(player, pipe.getSinkInv());
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof FluidSinkPipe)) return null;
		FluidSinkPipe pipe = (FluidSinkPipe) tile.pipe;
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.getSinkInv());
		dummy.addFluidSlot(0, pipe.getSinkInv(), 28, 15);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidBasicGui(getId());
	}
}
