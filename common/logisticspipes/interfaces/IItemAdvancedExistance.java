package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

public interface IItemAdvancedExistance {

	boolean canExistInNormalInventory(ItemStack stack);

	boolean canExistInWorld(ItemStack stack);
}
