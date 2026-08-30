package logisticspipes.network.guis.module.inpipe;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.gui.module.ProviderGui;
import network.rs485.logisticspipes.inventory.ProviderMode;
import network.rs485.logisticspipes.inventory.container.ProviderContainer;
import network.rs485.logisticspipes.property.layer.SimplePropertyOverlay;

@StaticResolve
public class ProviderModuleGuiProvider extends ModuleCoordinatesGuiProvider {

	@Getter
	@Setter
	private boolean exclude;

	@Getter
	@Setter
	private int extractorMode;

	@Getter
	@Setter
	private boolean isActive;

	@Getter
	@Setter
	private @Nullable Direction sneakyOrientation;

	public ProviderModuleGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		ModuleProvider module = this.getLogisticsModule(player.level(), ModuleProvider.class);
		if (module == null) {
			return null;
		}
		module.isExclusionFilter.setValue(exclude);
		module.providerMode.setValue(ProviderMode.modeFromIntSafe(extractorMode));
		module.setSneakyDirection(sneakyOrientation);
		module.isActive.setValue(isActive);
		return ProviderGui.create(player.getInventory(), module, ItemStack.EMPTY);
	}

	@Override
	public ProviderContainer getContainer(Player player) {
		ModuleProvider module = this.getLogisticsModule(player.level(), ModuleProvider.class);
		if (module == null) {
			return null;
		}
		return new ProviderContainer(module, player.getInventory(), new SimplePropertyOverlay<>(module.filterInventory), ItemStack.EMPTY);
	}

	@Override
	public GuiProvider template() {
		return new ProviderModuleGuiProvider(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBoolean(exclude);
		output.writeInt(extractorMode);
		output.writeBoolean(isActive);
		output.writeFacing(sneakyOrientation);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		exclude = input.readBoolean();
		extractorMode = input.readInt();
		isActive = input.readBoolean();
		sneakyOrientation = input.readFacing();
	}
}
