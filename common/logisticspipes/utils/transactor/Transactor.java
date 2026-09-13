package logisticspipes.utils.transactor;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

public abstract class Transactor implements ITransactor {

	@Override
    public ItemStack add(ItemStack stack, @Nullable Direction orientation, boolean doAdd) {
		ItemStack added = stack.copy();
		added.setCount(inject(stack, orientation, doAdd));
		return added;
	}

	public abstract int inject(ItemStack stack, @Nullable Direction orientation, boolean doAdd);
}
