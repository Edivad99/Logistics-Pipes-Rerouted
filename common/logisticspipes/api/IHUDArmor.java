package logisticspipes.api;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

public interface IHUDArmor {

	boolean isEnabled(@Nonnull ItemStack item);
}
