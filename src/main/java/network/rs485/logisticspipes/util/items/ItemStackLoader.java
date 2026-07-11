package network.rs485.logisticspipes.util.items;

import javax.annotation.Nonnull;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class ItemStackLoader {

	@Nonnull
	public static ItemStack loadAndFixItemStackFromNBT(CompoundTag nbt, HolderLookup.Provider provider) {
		// In 1.20, ItemStack.of(CompoundTag) handles data-fixer upgrades internally via Util.getFixerUpper().
		return ItemStack.parse(provider, nbt).orElse(ItemStack.EMPTY);
	}
}
