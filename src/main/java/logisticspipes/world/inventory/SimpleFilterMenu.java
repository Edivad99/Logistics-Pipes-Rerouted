package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;

import logisticspipes.modules.SimpleFilter;

/**
 * The nine filter slots shared by every module that filters by plain item.
 */
public class SimpleFilterMenu extends ModuleMenu {

    public SimpleFilterMenu(int containerId, Inventory inventory, ModuleTarget target, SimpleFilter filter) {
        super(LPMenuTypes.SIMPLE_FILTER.get(), containerId, inventory, target, (LogisticsModule) filter);
        addNormalSlotsForPlayerInventory(inventory, 8, 60);
        for (int slot = 0; slot < 9; slot++) {
            addDummySlot(slot, filter.getFilterInventory(), 8 + slot * 18, 18);
        }
    }
}
