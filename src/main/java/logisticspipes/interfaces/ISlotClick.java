package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

/**
 * Supplies what a result slot hands over when clicked, which is made at that moment.
 */
public interface ISlotClick {

    /**
     * @return the result, or an empty stack when there is none
     */
    ItemStack getResultForClick();
}
