package logisticspipes.util;

import logisticspipes.LPConfigs;

public final class ModuleUtil {

    private ModuleUtil() {
    }

    /**
     * Checks inventory size, everyNthTick and configuration values
     * to determine the number of slot accesses per tick.
     *
     * @param inventorySize the size of the connected inventory.
     * @return 0, if no work can be done and a value greater zero otherwise.
     */
    public static int determineSlotsPerTick(int everyNthTick, int inventorySize) {
        int slotsPerTick = 0;
        if (inventorySize > 0) {
            slotsPerTick = Math.max(
                inventorySize / everyNthTick,
                LPConfigs.COMMON.MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK.getAsInt());
        }
        int maximum = LPConfigs.COMMON.MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK.getAsInt();
        if (maximum > 0) {
            slotsPerTick = Math.min(slotsPerTick, maximum);
        }
        return slotsPerTick;
    }
}
