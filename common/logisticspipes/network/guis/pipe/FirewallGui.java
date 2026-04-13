package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.GuiFirewall;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class FirewallGui extends CoordinatesGuiProvider {

	public FirewallGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeItemsFirewall)) return null;
		return new GuiFirewall((PipeItemsFirewall) tile.pipe, player);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null || !(tile.pipe instanceof PipeItemsFirewall)) return null;
		PipeItemsFirewall pipe = (PipeItemsFirewall) tile.pipe;
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.inv);
		dummy.addNormalSlotsForPlayerInventory(33, 147);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				dummy.addDummySlot(i * 6 + j, i * 18 + 17, j * 18 + 41);
			}
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FirewallGui(getId());
	}
}
