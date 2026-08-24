package logisticspipes.utils.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import logisticspipes.interfaces.ISlotCheck;

public class RestrictedSlot extends Slot {

	private final Item item;
	private final ISlotCheck slotCheck;

	public RestrictedSlot(Container container, int slot, int x, int y, Class<? extends Item> itemClass) {
		super(container, slot, x, y);
		this.item = null;
		slotCheck = itemStack -> !itemStack.isEmpty() && itemClass.isAssignableFrom(itemStack.getItem().getClass());
	}

	public RestrictedSlot(Container container, int slot, int x, int y, Item item) {
        super(container, slot, x, y);
		this.item = item;
		slotCheck = null;
	}

	public RestrictedSlot(Container container, int slot, int x, int y, ISlotCheck slotCheck) {
        super(container, slot, x, y);
		item = null;
		this.slotCheck = slotCheck;
	}

	/**
	 * Check if the stack is a valid item for this slot. Always true beside for
	 * the armor slots.
	 */
	@Override
	public boolean mayPlace(ItemStack par1ItemStack) {
		if (slotCheck == null) {
			return par1ItemStack.getItem() == item;
		} else {
			return slotCheck.isStackAllowed(par1ItemStack);
		}
	}
}
