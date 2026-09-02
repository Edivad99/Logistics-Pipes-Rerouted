package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeItemsFirewall;

/**
 * The 6x6 filter grid of a firewall pipe.
 */
public class FirewallMenu extends DummyMenu {

    @Getter
    private final PipeItemsFirewall pipe;

    public FirewallMenu(int containerId, Inventory inventory, PipeItemsFirewall pipe) {
        super(LPMenuTypes.FIREWALL.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        addNormalSlotsForPlayerInventory(inventory, 33, 175);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                addDummySlot(x * 6 + y, pipe.inv, x * 18 + 17, y * 18 + 41);
            }
        }
    }
}
