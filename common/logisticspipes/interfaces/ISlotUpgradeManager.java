package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.upgrades.IPipeUpgrade;

public interface ISlotUpgradeManager {

	boolean hasPatternUpgrade();

	boolean isAdvancedSatelliteCrafter();

	boolean hasByproductExtractor();

	int getFluidCrafter();

	boolean isFuzzyUpgrade();

	int getCrafterCleanup();

	boolean hasSneakyUpgrade();

	@Nullable
	Direction getSneakyOrientation();

	boolean hasOwnSneakyUpgrade();

	Container getInv();

	@Nullable
	IPipeUpgrade getUpgrade(int slot);

	BlockPos getPipePosition();

	int getActionSpeedUpgrade();

	int getItemExtractionUpgrade();

	int getItemStackExtractionUpgrade();
}
