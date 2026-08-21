package logisticspipes.pipes.upgrades;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.network.guis.upgrade.DisconnectionUpgradeConfigGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ConnectionUpgradeConfig implements IConfigPipeUpgrade {

	public static String getName() {
		return "disconnection";
	}

	@AllArgsConstructor
	public enum Sides {
		UP(Direction.UP, "LPDIS-UP"),
		DOWN(Direction.DOWN, "LPDIS-DOWN"),
		NORTH(Direction.NORTH, "LPDIS-NORTH"),
		SOUTH(Direction.SOUTH, "LPDIS-SOUTH"),
		EAST(Direction.EAST, "LPDIS-EAST"),
		WEST(Direction.WEST, "LPDIS-WEST");
		@Getter
		private Direction dir;
		@Getter
		private String lpName;

		public static String getNameForDirection(Direction fd) {
			Optional<Sides> opt = Arrays.stream(values()).filter(side -> side.getDir() == fd).findFirst();
			if (opt.isPresent()) {
				return opt.get().getLpName();
			}
			return "LPDIS-UNKNWON";
		}
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
		return true;
	}

	@Override
	public boolean isAllowedForModule(LogisticsModule pipe) {
		return false;
	}

	@Override
	public String[] getAllowedPipes() {
		return new String[] { "all" };
	}

	@Override
	public String[] getAllowedModules() {
		return new String[] {};
	}

	@Override
	public UpgradeCoordinatesGuiProvider getGUI() {
		return NewGuiHandler.getGui(DisconnectionUpgradeConfigGuiProvider.class);
	}

	public Stream<Direction> getSides(ItemStack stack) {
		if (stack.isEmpty()) return Stream.empty();
		final CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return Arrays.stream(Sides.values()).filter(side -> tag.getBooleanOr(side.getLpName(), false)).map(Sides::getDir);
	}
}
