package logisticspipes.interfaces;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

public interface IPipeUpgradeManager {

	boolean hasPowerPassUpgrade();

	boolean hasFEPowerSupplierUpgrade();

	int getSpeedUpgradeCount();

	boolean isSideDisconnected(Direction side);

	boolean hasCCRemoteControlUpgrade();

	boolean hasCraftingMonitoringUpgrade();

	boolean isOpaque();

	boolean hasUpgradeModuleUpgrade();

	boolean hasCombinedSneakyUpgrade();

	Direction @Nullable [] getCombinedSneakyOrientation();

}
