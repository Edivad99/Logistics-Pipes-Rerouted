package logisticspipes.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

public interface ISlotClick {

	@Nonnull
	ItemStack getResultForClick();
}
