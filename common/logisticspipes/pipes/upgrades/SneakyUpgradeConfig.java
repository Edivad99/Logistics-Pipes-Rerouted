package logisticspipes.pipes.upgrades;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.to_client.pipe.UpgradeConfigPopupMessage;

public class SneakyUpgradeConfig implements IConfigPipeUpgrade {

	public static final String SIDE_KEY = "LPSNEAKY-SIDE";

	public static String getName() {
		return "sneaky";
	}

	@AllArgsConstructor
	public enum Sides {
		UP(Direction.UP, "LPSNEAKY-UP"),
		DOWN(Direction.DOWN, "LPSNEAKY-DOWN"),
		NORTH(Direction.NORTH, "LPSNEAKY-NORTH"),
		SOUTH(Direction.SOUTH, "LPSNEAKY-SOUTH"),
		EAST(Direction.EAST, "LPSNEAKY-EAST"),
		WEST(Direction.WEST, "LPSNEAKY-WEST");

		@Getter
		private Direction dir;
		@Getter
		private String lpName;

		public static String getNameForDirection(Direction fd) {
			return Arrays.stream(values())
					.filter(side -> side.getDir() == fd)
					.map(Sides::getLpName)
					.findFirst()
					.orElse("LPSNEAKY-UNKNWON");
		}
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of(UpgradeTarget.ALL);
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of(UpgradeTarget.SINK);
	}

	@Override
	public UpgradeConfigPopupMessage.Kind getConfigPopup() {
		return UpgradeConfigPopupMessage.Kind.SNEAKY;
	}

	@Nullable
	public static Direction getSide(ItemStack stack) {
		if (stack.isEmpty()) return null;
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		String sideString = tag.getStringOr(SIDE_KEY, "");
		return Arrays.stream(Sides.values())
				.filter(side -> side.getLpName().equals(sideString))
				.map(Sides::getDir)
				.findFirst()
				.orElse(null);
	}
}
