package logisticspipes.network.guis.pipe;

import logisticspipes.gui.orderer.NormalMk2GuiOrderer;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class NormalMk2OrdererGui extends CoordinatesGuiProvider {

	public NormalMk2OrdererGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(tile.pipe instanceof PipeItemsRequestLogisticsMk2)) return null;
		return new NormalMk2GuiOrderer((PipeItemsRequestLogisticsMk2) tile.pipe, player);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeItemsRequestLogisticsMk2)) return null;
		return new DummyContainer(player.getInventory(), null);
	}

	@Override
	public GuiProvider template() {
		return new NormalMk2OrdererGui(getId());
	}
}
