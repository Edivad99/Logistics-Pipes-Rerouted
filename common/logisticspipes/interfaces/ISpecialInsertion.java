package logisticspipes.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

public interface ISpecialInsertion {

	int addToSlot(@Nonnull ItemStack stack, int i);
}
