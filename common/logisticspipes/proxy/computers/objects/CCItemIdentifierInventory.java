package logisticspipes.proxy.computers.objects;

import org.jspecify.annotations.Nullable;

import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCQueued;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.utils.item.ItemIdentifierStack;

@CCType(name = "FilterInventory")
public class CCItemIdentifierInventory {

	private final IItemIdentifierInventory inv;

	public CCItemIdentifierInventory(IItemIdentifierInventory inv) {
		this.inv = inv;
	}

	@CCCommand(description = "Returns the size of this FilterInventory")
	public int getContainerSize() {
		return inv.getContainerSize();
	}

	@CCCommand(description = "Returns the ItemIdentifierStack in the given slot")
	@CCQueued
	@Nullable
	public ItemIdentifierStack getItemIdentifierStack(Double slot) {
		int s = slot.intValue();
		if (s <= 0 || s > getContainerSize()) {
			throw new UnsupportedOperationException("Slot out of Inventory");
		}
		if (s != slot) {
			throw new UnsupportedOperationException("Slot not an Integer");
		}
		s--;
		return inv.getIDStackInSlot(s);
	}

	@CCCommand(description = "Sets the ItemIdentifierStack at the given slot")
	@CCQueued
	public void setItemIdentifierStack(Double slot, ItemIdentifierStack stack) {
		int s = slot.intValue();
		if (s <= 0 || s > getContainerSize()) {
			throw new UnsupportedOperationException("Slot out of Inventory");
		}
		if (s != slot) {
			throw new UnsupportedOperationException("Slot not an Integer");
		}
		s--;
		inv.setItem(s, stack);
	}

	@CCCommand(description = "Sets the ItemIdentifierStack at the given slot")
	@CCQueued
	public void clearSlot(Double slot) {
		int s = slot.intValue();
		if (s <= 0 || s > getContainerSize()) {
			throw new UnsupportedOperationException("Slot out of Inventory");
		}
		if (s != slot) {
			throw new UnsupportedOperationException("Slot not an Integer");
		}
		s--;
		inv.setItem(s, (ItemIdentifierStack) null);
	}
}
