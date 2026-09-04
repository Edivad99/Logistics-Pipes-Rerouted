package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

/**
 * The player's own settings screen, which shows their inventory and nothing else.
 */
public class PlayerSettingsMenu extends DummyMenu {

    public PlayerSettingsMenu(int containerId, Inventory inventory) {
        super(LPMenuTypes.PLAYER_SETTINGS.get(), containerId, inventory.player, null);
        addNormalSlotsForPlayerInventory(inventory, 10, 95);
    }
}
