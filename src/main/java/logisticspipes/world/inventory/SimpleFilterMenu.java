package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;

import logisticspipes.modules.SimpleFilter;

/**
 * The nine filter slots shared by every module that filters by plain item.
 *
 * <p>Takes its own type: the same nine slots back more than one screen.
 */
public class SimpleFilterMenu extends ModuleMenu {

    public SimpleFilterMenu(MenuType<?> menuType, int containerId, Inventory inventory, ModuleTarget target,
        SimpleFilter filter) {
        super(menuType, containerId, inventory, target, (LogisticsModule) filter);
        addNormalSlotsForPlayerInventory(inventory, 8, 60);
        for (int slot = 0; slot < 9; slot++) {
            addDummySlot(slot, filter.getFilterInventory(), 8 + slot * 18, 18);
        }
    }
}
