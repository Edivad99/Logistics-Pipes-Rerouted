package logisticspipes.utils.transactor;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

public interface ITransactor {

	/**
	 * Adds stack to the transactor
	 * @param stack to add.
	 * @param orientation side where transaction is being performed
	 * @param doAdd whether to commit or simulate transaction
	 * @return added stack.
	 */
    ItemStack add(ItemStack stack, @Nullable Direction orientation, boolean doAdd);
}
