package logisticspipes.utils.transactor;

import java.util.Iterator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import logisticspipes.utils.transfer.ItemHandlers;


class InventoryIteratorSimple implements Iterable<IInvSlot> {

	private final ResourceHandler<ItemResource> inv;

	InventoryIteratorSimple(ResourceHandler<ItemResource> inv) {
		this.inv = inv;
	}

	@Override
	public Iterator<IInvSlot> iterator() {
		return new Iterator<IInvSlot>() {

			int slot = 0;

			@Override
			public boolean hasNext() {
				return slot < inv.size();
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
			return ItemUtil.getStack(inv, slot);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			return ItemUtil.insertItemReturnRemaining(inv, slot, stack.copy(), simulate, null);
		}

		@Override
		public ItemStack extractItem(int amount, boolean simulate) {
			return ItemHandlers.extractItem(inv, slot, amount, simulate);
		}

		@Override
		public int getSlotLimit() {
			return ItemHandlers.slotLimit(inv, slot);
		}

		@Override
		public boolean canPutStackInSlot(ItemStack stack) {
			ItemStack toTest = stack.copy();
			toTest.setCount(1);
			return ItemUtil.insertItemReturnRemaining(inv, slot, toTest, true, null).isEmpty();
		}
	}
}
