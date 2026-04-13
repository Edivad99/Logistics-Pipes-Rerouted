package logisticspipes.utils.transactor;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;

public abstract class Transactor implements ITransactor {

	@Override
	@Nonnull
	public ItemStack add(@Nonnull ItemStack stack, Direction orientation, boolean doAdd) {
		ItemStack added = stack.copy();
		added.setCount(inject(stack, orientation, doAdd));
		return added;
	}

	public abstract int inject(@Nonnull ItemStack stack, Direction orientation, boolean doAdd);
}
