package logisticspipes.interfaces;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

/**
 * The upgrades installed in a pipe, as far as the pipe itself is concerned.
 *
 * @see ISlotUpgradeManager for what its modules see
 */
public interface IPipeUpgradeManager {

    boolean hasPowerPassUpgrade();

    boolean hasFEPowerSupplierUpgrade();

    int getSpeedUpgradeCount();

    boolean isSideDisconnected(Direction side);

    boolean hasCCRemoteControlUpgrade();

    boolean hasCraftingMonitoringUpgrade();

    /**
     * @return whether the pipe hides what travels through it
     */
    boolean isOpaque();

    boolean hasUpgradeModuleUpgrade();

    boolean hasCombinedSneakyUpgrade();

    /**
     * @return the sides to try inserting from, in order; the array may be null, and so may any side
     * in it
     */
    @Nullable Direction @Nullable [] getCombinedSneakyOrientation();
}
