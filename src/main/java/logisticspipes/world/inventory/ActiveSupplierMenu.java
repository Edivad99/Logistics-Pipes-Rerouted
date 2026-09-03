package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.network.ModuleTarget;

/**
 * The active supplier's filter: a 3x3 grid, or a row of nine when the pattern upgrade turns it
 * into one slot per position of the target inventory.
 */
public class ActiveSupplierMenu extends ModuleMenu {

    @Getter
    private final ModuleActiveSupplier supplier;

    @Getter
    private final boolean patternUpgrade;

    public ActiveSupplierMenu(int containerId, Inventory inventory, ModuleTarget target,
        ModuleActiveSupplier supplier, boolean patternUpgrade) {
        super(LPMenuTypes.ACTIVE_SUPPLIER.get(), containerId, inventory, target, supplier);
        this.supplier = supplier;
        this.patternUpgrade = patternUpgrade;
        addNormalSlotsForPlayerInventory(inventory, 18, 97);
        if (patternUpgrade) {
            for (int slot = 0; slot < 9; slot++) {
                addDummySlot(slot, supplier.inventory, 18 + slot * 18, 20);
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    addDummySlot(column + row * 3, supplier.inventory, 72 + column * 18, 18 + row * 18);
                }
            }
        }
    }
}
