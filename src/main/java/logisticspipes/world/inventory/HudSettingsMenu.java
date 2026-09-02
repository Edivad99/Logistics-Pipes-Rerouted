package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

/**
 * The HUD glasses' settings screen.
 *
 * <p>Belongs to an item rather than to a block, so what it carries is the inventory slot the
 * glasses were used from.
 */
public class HudSettingsMenu extends DummyMenu {

    @Getter
    private final int slot;

    public HudSettingsMenu(int containerId, Inventory inventory, int slot) {
        super(LPMenuTypes.HUD_SETTINGS.get(), containerId, inventory.player, null);
        this.slot = slot;
        addRestrictedHotbarForPlayerInventory(inventory, 10, 134);
        addRestrictedArmorForPlayerInventory(inventory, 10, 65);
    }
}
