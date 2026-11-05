package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

/**
 * An inventory an item can be put into at a given slot.
 */
public interface ISpecialInsertion {

    /**
     * @return how many of the stack went in
     */
    int addToSlot(ItemStack stack, int slot);
}
