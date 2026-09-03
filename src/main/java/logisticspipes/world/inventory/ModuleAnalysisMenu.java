package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;
import logisticspipes.utils.item.ItemIdentifierInventory;

/**
 * A module configured by a list of names, with one slot to drop an item in and read its names off.
 *
 * <p>The slot holds nothing: it is a place to show an item so the screen can offer what it is
 * called. The screens used to pass it to themselves through a static field, because the container
 * had to be built before {@code super()} could run.
 */
public class ModuleAnalysisMenu extends ModuleMenu {

    @Getter
    private final ItemIdentifierInventory analysisInventory = new ItemIdentifierInventory(1, "Analyse Slot", 1);

    public ModuleAnalysisMenu(MenuType<?> menuType, int containerId, Inventory inventory, ModuleTarget target,
        LogisticsModule module) {
        super(menuType, containerId, inventory, target, module);
        addDummySlot(0, analysisInventory, 7, 8);
        addNormalSlotsForPlayerInventory(inventory, 7, 126);
    }
}
