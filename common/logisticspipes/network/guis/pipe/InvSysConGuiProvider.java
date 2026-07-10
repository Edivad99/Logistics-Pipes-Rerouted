package logisticspipes.network.guis.pipe;

import logisticspipes.gui.GuiInvSysConnector;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class InvSysConGuiProvider extends CoordinatesGuiProvider {

	public InvSysConGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeItemsInvSysConnector)) {
			return null;
		}
		return new GuiInvSysConnector(player, (PipeItemsInvSysConnector) pipe.pipe);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeItemsInvSysConnector)) {
			return null;
		}
		DummyContainer dummy = new DummyContainer(player, null, (PipeItemsInvSysConnector) pipe.pipe);

		dummy.addNormalSlotsForPlayerInventory(0, 50);

		return dummy;

	}

	@Override
	public GuiProvider template() {
		return new InvSysConGuiProvider(getId());
	}
}
