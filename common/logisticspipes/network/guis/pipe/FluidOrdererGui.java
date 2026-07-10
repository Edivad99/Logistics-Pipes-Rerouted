package logisticspipes.network.guis.pipe;

import logisticspipes.gui.orderer.FluidGuiOrderer;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FluidOrdererGui extends CoordinatesGuiProvider {

	public FluidOrdererGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(tile.pipe instanceof PipeFluidRequestLogistics)) return null;
		return new FluidGuiOrderer((PipeFluidRequestLogistics) tile.pipe, player);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeFluidRequestLogistics)) return null;
		return new DummyContainer(player.getInventory(), null);
	}

	@Override
	public GuiProvider template() {
		return new FluidOrdererGui(getId());
	}
}
