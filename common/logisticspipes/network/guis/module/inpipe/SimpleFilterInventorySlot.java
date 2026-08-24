package logisticspipes.network.guis.module.inpipe;

import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

import logisticspipes.gui.modules.GuiSimpleFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.module.SimpleFilter;

@StaticResolve
public class SimpleFilterInventorySlot extends ModuleCoordinatesGuiProvider {

	public SimpleFilterInventorySlot(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsModule module = this.getLogisticsModule(player.level(), LogisticsModule.class);
		if (module == null) {
			return null;
		}
		return new GuiSimpleFilter(player.getInventory(), module);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		return getContainerFromFilterModule(this, player);
	}

	@Nullable
	public static DummyContainer getContainerFromFilterModule(ModuleCoordinatesGuiProvider guiProvider, Player player) {
		SimpleFilter filter = guiProvider.getLogisticsModule(player.level(), SimpleFilter.class);
		if (filter == null) {
			return null;
		}
		DummyContainer dummy = new DummyContainer(player.getInventory(), filter.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}

		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new SimpleFilterInventorySlot(getId());
	}
}
