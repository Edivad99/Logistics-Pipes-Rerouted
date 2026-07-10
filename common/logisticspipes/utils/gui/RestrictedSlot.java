package logisticspipes.utils.gui;

import javax.annotation.Nonnull;
import logisticspipes.interfaces.ISlotCheck;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RestrictedSlot extends Slot {

	private final Item item;
	private final ISlotCheck slotCheck;

	public RestrictedSlot(Container iinventory, int i, int j, int k, Class<? extends Item> itemClass) {
		super(iinventory, i, j, k);
		this.item = null;
		slotCheck = itemStack -> !itemStack.isEmpty() && itemClass.isAssignableFrom(itemStack.getItem().getClass());
	}

	public RestrictedSlot(Container iinventory, int i, int j, int k, Item item) {
		super(iinventory, i, j, k);
		this.item = item;
		slotCheck = null;
	}

	public RestrictedSlot(Container iinventory, int i, int j, int k, ISlotCheck slotCheck) {
		super(iinventory, i, j, k);
		item = null;
		this.slotCheck = slotCheck;
	}

	/**
	 * Check if the stack is a valid item for this slot. Always true beside for
	 * the armor slots.
	 */
	@Override
	public boolean mayPlace(@Nonnull ItemStack par1ItemStack) {
		if (slotCheck == null) {
			return par1ItemStack.getItem() == item;
		} else {
			return slotCheck.isStackAllowed(par1ItemStack);
		}
	}
}
