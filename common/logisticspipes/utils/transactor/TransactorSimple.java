package logisticspipes.utils.transactor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import logisticspipes.utils.transfer.ItemHandlers;

public class TransactorSimple extends Transactor {

	protected ResourceHandler<ItemResource> inventory;

	public TransactorSimple(ResourceHandler<ItemResource> inventory) {
		this.inventory = inventory;
	}

	@Override
	public int inject(ItemStack stack, Direction orientation, boolean doAdd) {
		List<IInvSlot> filledSlots = new ArrayList<>(inventory.size());
		List<IInvSlot> emptySlots = new ArrayList<>(inventory.size());
		for (IInvSlot slot : InventoryIterator.getIterable(inventory, orientation)) {
			if (slot.canPutStackInSlot(stack)) {
				if (slot.getItem().isEmpty()) {
					emptySlots.add(slot);
				} else {
					filledSlots.add(slot);
				}
			}
		}

		int injected = 0;
		injected = tryPut(stack, filledSlots, injected, doAdd);
		injected = tryPut(stack, emptySlots, injected, doAdd);

		return injected;
	}

	private int tryPut(ItemStack stack, List<IInvSlot> slots, int injected, boolean doAdd) {
		int realInjected = injected;

		if (realInjected >= stack.getCount()) {
			return realInjected;
		}

		for (IInvSlot slot : slots) {
			ItemStack stackInSlot = slot.getItem();
			if (stackInSlot.isEmpty() || canStacksMerge(stackInSlot, stack)) {
				int used = addToSlot(slot, stack, realInjected, doAdd);
				if (used > 0) {
					realInjected += used;
					if (realInjected >= stack.getCount()) {
						return realInjected;
					}
				}
			}
		}

		return realInjected;
	}

	/**
	 * @param slot
	 * @param stack
	 * @param injected Amount not to move?
	 * @param doAdd
	 * @return Return the number of items moved.
	 */
	protected int addToSlot(IInvSlot slot, ItemStack stack, int injected, boolean doAdd) {
		int available = stack.getCount() - injected;

		ItemStack newStack = stack.copy();
		newStack.setCount(available);

		ItemStack rest = slot.insertItem(newStack, !doAdd);
		return available - rest.getCount();
	}

	private boolean canStacksMerge(ItemStack stack1, ItemStack stack2) {
		if (stack1.isEmpty() || stack2.isEmpty()) {
			return false;
		}
		if (!ItemStack.isSameItem(stack1, stack2)) {
			return false;
		}
		return ItemStack.isSameItemSameComponents(stack1, stack2);
	}
}
