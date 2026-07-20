package logisticspipes.utils.transactor;

import java.util.Iterator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;


class InventoryIteratorSimple implements Iterable<IInvSlot> {

	private final IItemHandler inv;

	InventoryIteratorSimple(IItemHandler inv) {
		this.inv = inv;
	}

	@Override
	public Iterator<IInvSlot> iterator() {
		return new Iterator<IInvSlot>() {

			int slot = 0;

			@Override
			public boolean hasNext() {
				return slot < inv.getSlots();
			}

			@Override
			public IInvSlot next() {
				return new InvSlot(slot++);
			}

		};
	}

	private class InvSlot implements IInvSlot {

		private final int slot;

		public InvSlot(int slot) {
			this.slot = slot;
		}

		@Override
		public ItemStack getItem() {
			return inv.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			return inv.insertItem(slot, stack.copy(), simulate);
		}

		@Override
		public ItemStack extractItem(int amount, boolean simulate) {
			return inv.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit() {
			return inv.getSlotLimit(slot);
		}

		@Override
		public boolean canPutStackInSlot(ItemStack stack) {
			ItemStack toTest = stack.copy();
			toTest.setCount(1);
			return inv.insertItem(slot, toTest, true).isEmpty();
		}
	}
}
