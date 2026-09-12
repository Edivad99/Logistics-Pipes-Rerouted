package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.ModuleTarget;

/** The provider module's 3x3 filter grid. */
public class ProviderMenu extends ModuleMenu {

    /** Must match ProviderScreen's layout. */
    private static final int FILTER_X = 61;
    private static final int FILTER_Y = 29;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 113;

    public ProviderMenu(int containerId, Inventory inventory, ModuleTarget target, ModuleProvider module) {
        super(LPMenuTypes.PROVIDER.get(), containerId, inventory, target, module);
        addNormalSlotsForPlayerInventory(inventory, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addDummySlot(column + row * 3, module.filterInventory, FILTER_X + column * 18, FILTER_Y + row * 18);
            }
        }
    }

    public ModuleProvider getProviderModule() {
        return (ModuleProvider) getModule();
    }
}
