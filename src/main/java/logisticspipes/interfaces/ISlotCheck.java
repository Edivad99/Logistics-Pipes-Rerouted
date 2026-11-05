package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

/**
 * Decides what a restricted slot accepts.
 */
public interface ISlotCheck {

    boolean isStackAllowed(ItemStack stack);
}
