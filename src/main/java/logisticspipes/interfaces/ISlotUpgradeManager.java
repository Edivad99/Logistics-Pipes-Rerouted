package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.upgrades.IPipeUpgrade;

/**
 * The upgrades that apply to a module: those of its own slot on a chassis, or those of its pipe.
 *
 * <p>The methods returning a number count the upgrades of that kind installed.</p>
 */
public interface ISlotUpgradeManager {

    boolean hasPatternUpgrade();

    boolean isAdvancedSatelliteCrafter();

    boolean hasByproductExtractor();

    int getFluidCrafter();

    boolean isFuzzyUpgrade();

    int getCrafterCleanup();

    boolean hasSneakyUpgrade();

    /**
     * @return the side set on the sneaky upgrade, or null when there is none
     */
    @Nullable Direction getSneakyOrientation();

    /**
     * @return whether the sneaky upgrade is in this module's slot, rather than in the pipe
     */
    boolean hasOwnSneakyUpgrade();

    Container getInv();

    /**
     * @return the upgrade in the slot, or null when it is empty
     */
    @Nullable IPipeUpgrade getUpgrade(int slot);

    BlockPos getPipePosition();

    int getActionSpeedUpgrade();

    int getItemExtractionUpgrade();

    int getItemStackExtractionUpgrade();
}
