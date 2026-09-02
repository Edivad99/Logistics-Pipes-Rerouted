package logisticspipes.network.guis.module.inpipe;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

import logisticspipes.gui.modules.GuiSneakyConfigurator;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.module.SneakyDirection;

@StaticResolve
public class SneakyModuleInSlotGuiProvider extends ModuleCoordinatesGuiProvider {

	private @Nullable Direction sneakyOrientation;

	public SneakyModuleInSlotGuiProvider(int id) {
		super(id);
	}

	public SneakyModuleInSlotGuiProvider setSneakyOrientation(@Nullable Direction sneakyOrientation) {
		this.sneakyOrientation = sneakyOrientation;
		return this;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(sneakyOrientation);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		sneakyOrientation = input.readFacing();
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsModule module = this.getLogisticsModule(player.level(), LogisticsModule.class);
		if (!(module instanceof SneakyDirection && module.hasGui())) {
			return null;
		}
		((SneakyDirection) module).setSneakyDirection(sneakyOrientation);
		return new GuiSneakyConfigurator(player.getInventory(), module);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsModule module = this.getLogisticsModule(player.level(), LogisticsModule.class);
		if (!(module instanceof SneakyDirection && module.hasGui())) {
			return null;
		}
		return new DummyContainer(player.getInventory(), null);
	}

	@Override
	public GuiProvider template() {
		return new SneakyModuleInSlotGuiProvider(getId());
	}
}
