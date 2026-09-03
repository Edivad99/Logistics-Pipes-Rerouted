package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;

/**
 * The side picker of a module that extracts from one face of an inventory.
 *
 * <p>Has no slots at all, not even the player's: the screen is six buttons.
 */
public class SneakyDirectionMenu extends ModuleMenu {

    public SneakyDirectionMenu(int containerId, Inventory inventory, ModuleTarget target, LogisticsModule module) {
        super(LPMenuTypes.SNEAKY_DIRECTION.get(), containerId, inventory, target, module);
    }
}
