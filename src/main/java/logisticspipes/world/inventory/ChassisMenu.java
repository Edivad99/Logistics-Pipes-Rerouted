package logisticspipes.world.inventory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.world.item.ItemUpgrade;

/**
 * The chassis screen: one module slot per chassis slot, and with the upgrade module upgrade
 * fitted, two upgrade slots beside each of them.
 */
public class ChassisMenu extends DummyMenu {

    @Getter
    private final PipeLogisticsChassis pipe;

    @Getter
    private final Container moduleInventory;

    /**
     * Whether the pipe carries the upgrade that gives each module its own upgrade slots. It decides
     * how many slots the menu has, so it travels with the menu rather than being read from the
     * pipe: the client's copy does not have the pipe's upgrade manager.
     */
    private final boolean hasUpgradeModuleUpgrade;

    /** Two per chassis slot, in slot order; empty without the upgrade module upgrade. */
    @Getter
    private final List<Slot> upgradeSlots = new ArrayList<>();

    public ChassisMenu(int containerId, Inventory inventory, PipeLogisticsChassis pipe,
        boolean hasUpgradeModuleUpgrade) {
        super(LPMenuTypes.CHASSIS.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        this.hasUpgradeModuleUpgrade = hasUpgradeModuleUpgrade;
        this.moduleInventory = pipe.getModuleInventory(inventory.player.registryAccess());
        final int size = pipe.getChassisSize();

        addNormalSlotsForPlayerInventory(inventory, 19, 10 + 20 * size);
        for (int slot = 0; slot < size; slot++) {
            addModuleSlot(slot, moduleInventory, 18, 9 + 20 * slot, pipe);
        }

        if (hasUpgradeModuleUpgrade) {
            for (int slot = 0; slot < size; slot++) {
                final int moduleSlot = slot;
                final ModuleUpgradeManager upgrades = pipe.getModuleUpgradeManager(slot);
                upgradeSlots.add(addUpgradeSlot(0, upgrades, 0, 145, 9 + slot * 20,
                    stack -> allowsUpgrade(stack, pipe, moduleSlot)));
                upgradeSlots.add(addUpgradeSlot(1, upgrades, 1, 165, 9 + slot * 20,
                    stack -> allowsUpgrade(stack, pipe, moduleSlot)));
            }
        }
    }

    /** Whether each module has its own two upgrade slots beside it. */
    public boolean hasUpgradeModuleUpgrade() {
        return hasUpgradeModuleUpgrade;
    }

    /**
     * An upgrade slot beside a module only takes upgrades that module knows what to do with, so an
     * empty chassis slot takes none at all.
     */
    private static boolean allowsUpgrade(ItemStack stack, PipeLogisticsChassis pipe, int moduleSlot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemUpgrade upgrade)) {
            return false;
        }
        final LogisticsModule module = pipe.getModules().getModule(moduleSlot);
        return module != null && upgrade.getUpgradeForItem(stack, null).isAllowedForModule(module);
    }
}
