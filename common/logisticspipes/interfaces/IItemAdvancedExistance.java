package logisticspipes.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

public interface IItemAdvancedExistance {

	boolean canExistInNormalInventory(@Nonnull ItemStack stack);

	boolean canExistInWorld(@Nonnull ItemStack stack);
}
