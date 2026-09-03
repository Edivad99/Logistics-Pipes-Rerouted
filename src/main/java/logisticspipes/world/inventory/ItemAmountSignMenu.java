package logisticspipes.world.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.signs.ItemAmountPipeSign;

/**
 * The screen that picks which item an amount sign counts: one ghost slot for that item.
 */
public class ItemAmountSignMenu extends DummyMenu {

    @Getter
    private final ItemAmountPipeSign sign;

    public ItemAmountSignMenu(int containerId, Inventory inventory, CoreRoutedPipe pipe, Direction side) {
        super(LPMenuTypes.ITEM_AMOUNT_SIGN.get(), containerId, inventory.player, pipe.container);
        if (!(pipe.getPipeSign(side) instanceof ItemAmountPipeSign amountSign)) {
            throw new IllegalStateException("No item amount sign on side %s of [%s]".formatted(side, pipe.getPos()));
        }
        this.sign = amountSign;
        addDummySlot(0, amountSign.itemTypeInv, 10, 13);
        addNormalSlotsForPlayerInventory(inventory, 11, 41);
    }
}
