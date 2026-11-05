package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

/**
 * An item that is not free to go everywhere an item normally can.
 */
public interface IItemAdvancedExistence {

    /**
     * @return false when a pipe must destroy the item rather than put it into a normal inventory
     */
    boolean canExistInNormalInventory(ItemStack stack);

    /**
     * @return false when the item must not be dropped: tossing it puts it back in the inventory
     */
    boolean canExistInWorld(ItemStack stack);
}
