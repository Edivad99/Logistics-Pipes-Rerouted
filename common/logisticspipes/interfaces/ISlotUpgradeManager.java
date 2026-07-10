package logisticspipes.interfaces;

import logisticspipes.pipes.upgrades.IPipeUpgrade;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface ISlotUpgradeManager {

	boolean hasPatternUpgrade();

	boolean isAdvancedSatelliteCrafter();

	boolean hasByproductExtractor();

	int getFluidCrafter();

	boolean isFuzzyUpgrade();

	int getCrafterCleanup();

	boolean hasSneakyUpgrade();

	Direction getSneakyOrientation();

	boolean hasOwnSneakyUpgrade();

	Container getInv();

	IPipeUpgrade getUpgrade(int slot);

	DoubleCoordinates getPipePosition();

	int getActionSpeedUpgrade();

	int getItemExtractionUpgrade();

	int getItemStackExtractionUpgrade();
}
