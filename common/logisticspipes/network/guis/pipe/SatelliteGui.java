package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.GuiSatellitePipe;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.SatellitePipe;

@StaticResolve
public class SatelliteGui extends CoordinatesGuiProvider {

	public SatelliteGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(tile.pipe instanceof SatellitePipe)) return null;
		return new GuiSatellitePipe((SatellitePipe) tile.pipe);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof SatellitePipe)) return null;
		return new DummyContainer(player.getInventory(), null);
	}

	@Override
	public GuiProvider template() {
		return new SatelliteGui(getId());
	}
}
