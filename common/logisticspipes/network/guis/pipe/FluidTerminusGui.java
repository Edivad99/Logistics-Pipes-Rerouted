package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.GuiFluidTerminus;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class FluidTerminusGui extends CoordinatesGuiProvider {

	public FluidTerminusGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeFluidTerminus)) return null;
		return new GuiFluidTerminus(player, (PipeFluidTerminus) tile.pipe);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeFluidTerminus)) return null;
		PipeFluidTerminus pipe = (PipeFluidTerminus) tile.pipe;
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.getSinkInv());
		for (int i = 0; i < 9; i++) {
			dummy.addFluidSlot(i, pipe.getSinkInv(), 8 + i * 18, 13);
		}
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidTerminusGui(getId());
	}
}
