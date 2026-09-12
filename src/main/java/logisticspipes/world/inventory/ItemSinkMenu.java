package logisticspipes.world.inventory;

import java.util.EnumSet;

import net.minecraft.world.entity.player.Inventory;

import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.ModuleTarget;
import network.rs485.logisticspipes.util.FuzzyFlag;

/**
 * The item sink's nine filter slots.
 *
 * <p>Its own type rather than {@link SimpleFilterMenu}: the filter slots are fuzzy when the pipe
 * carries a fuzzy upgrade, and that is only known while the menu is being built.
 */
public class ItemSinkMenu extends ModuleMenu {

    /** The only two flags {@link ModuleItemSink} looks at when matching. */
    private static final EnumSet<FuzzyFlag> USED_FLAGS = EnumSet.of(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT);

    /** Must match ItemSinkScreen's layout. */
    private static final int FILTER_X = 8;
    private static final int FILTER_Y = 19;
    private static final int PLAYER_INVENTORY_Y = 67;

    public ItemSinkMenu(int containerId, Inventory inventory, ModuleTarget target, ModuleItemSink module,
        boolean fuzzy) {
        super(LPMenuTypes.ITEM_SINK.get(), containerId, inventory, target, module);
        addNormalSlotsForPlayerInventory(inventory, FILTER_X, PLAYER_INVENTORY_Y);
        for (int slot = 0; slot < 9; slot++) {
            int x = FILTER_X + slot * 18;
            if (fuzzy) {
                addFuzzyDummySlot(slot, module.getFilterInventory(), x, FILTER_Y, module.getSlotFuzzyFlags(slot),
                    USED_FLAGS);
            } else {
                addDummySlot(slot, module.getFilterInventory(), x, FILTER_Y);
            }
        }
    }

    public ModuleItemSink getItemSinkModule() {
        return (ModuleItemSink) getModule();
    }
}
