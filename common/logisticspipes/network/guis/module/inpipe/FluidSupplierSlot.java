package logisticspipes.network.guis.module.inpipe;

import logisticspipes.gui.modules.GuiFluidSupplier;
import logisticspipes.modules.ModuleFluidSupplier;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class FluidSupplierSlot extends ModuleCoordinatesGuiProvider {

	public FluidSupplierSlot(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		ModuleFluidSupplier module = this.getLogisticsModule(player.level(), ModuleFluidSupplier.class);
		return new GuiFluidSupplier(player.getInventory(), module);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		ModuleFluidSupplier module = this.getLogisticsModule(player.level(), ModuleFluidSupplier.class);
		DummyContainer dummy = new DummyContainer(player.getInventory(), module.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);
		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FluidSupplierSlot(getId());
	}
}
