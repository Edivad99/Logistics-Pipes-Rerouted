/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.LinkedList;
import javax.annotation.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.tuples.Triplet;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ItemIdentifierStack implements Comparable<ItemIdentifierStack>, ILPCCTypeHolder {

	private final Object[] ccTypeHolder = new Object[1];
	private final ItemIdentifier _item;
	private int stackSize;

	public static ItemIdentifierStack getFromStack(ItemStack stack) {
		return new ItemIdentifierStack(ItemIdentifier.get(stack), stack.getCount());
	}

	/**
	 * Serializes this stack as <code>{ item: &lt;namespaced id + components&gt;, amount: int }</code>.
	 * <p>
	 * {@link ItemStack#SINGLE_ITEM_CODEC} rather than {@link ItemStack#CODEC}: the latter caps the
	 * count at 99, and the amounts stored here routinely exceed that, so the count is kept as a
	 * separate field. Note that the codec drops transient components, matching vanilla's own
	 * persistence behaviour.
	 */
	public CompoundTag saveToNBT(HolderLookup.Provider provider) {
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		CompoundTag entry = new CompoundTag();
		entry.put("item", ItemStack.SINGLE_ITEM_CODEC.encodeStart(ops, _item.makeNormalStack(1)).getOrThrow());
		entry.putInt("amount", getStackSize());
		return entry;
	}

	/**
	 * Reads back what {@link #saveToNBT} wrote, or null when the entry is unreadable.
	 */
	@Nullable
	public static ItemIdentifierStack loadFromNBT(CompoundTag entry, HolderLookup.Provider provider) {
		if (!entry.contains("item")) {
			if (entry.contains("id")) {
				LogisticsPipes.LOG.warn(
						"Skipping an item stored in the pre-DataComponents format (numeric item id + damage + nbt). "
								+ "It cannot be migrated automatically; re-save it to update the format.");
			}
			return null;
		}
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		return ItemStack.SINGLE_ITEM_CODEC.parse(ops, entry.get("item"))
				.resultOrPartial(error -> LogisticsPipes.LOG.error("Could not read stored item: {}", error))
				.map(stack -> new ItemIdentifierStack(ItemIdentifier.get(stack), entry.getInt("amount")))
				.orElse(null);
	}

	public ItemIdentifierStack(ItemIdentifier item, int stackSize) {
		_item = item;
		setStackSize(stackSize);
	}

	public ItemIdentifierStack(ItemIdentifierStack copy) {
		this(copy._item, copy.getStackSize());
	}

	public ItemIdentifier getItem() {
		return _item;
	}

	/**
	 * @return the stackSize
	 */
	public int getStackSize() {
		return stackSize;
	}

	/**
	 * @param stackSize
	 *            the stackSize to set
	 */
	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public void lowerStackSize(int stackSize) {
		this.stackSize -= stackSize;
	}

	public ItemStack makeNormalStack() {
		return _item.makeNormalStack(stackSize);
	}

	public ItemEntity makeEntityItem(Level level, double x, double y, double z) {
		return _item.makeEntityItem(stackSize, level, x, y, z);
	}

	@Override
	public boolean equals(Object that) {
		if (that instanceof ItemIdentifierStack) {
			ItemIdentifierStack stack = (ItemIdentifierStack) that;
			return stack._item.equals(_item) && stack.getStackSize() == getStackSize();
		}
		if ((that instanceof ItemIdentifier)) {
			throw new IllegalStateException("Comparison between ItemIdentifierStack and ItemIdentifier -- did you forget a .getItem() in your code?");
		}

		return false;
	}

	@Override
	public int hashCode() {
		return _item.hashCode() ^ (1023 * getStackSize());
	}

	@Override
	public String toString() {
		return String.format("%dx %s", getStackSize(), _item);
	}

	public String getFriendlyName() {
		return getStackSize() + " " + _item.getFriendlyName();
	}

	public static LinkedList<ItemIdentifierStack> getListFromInventory(Container inv) {
		return ItemIdentifierStack.getListFromInventory(inv, false);
	}

	public static LinkedList<ItemIdentifierStack> getListFromInventory(Container inv, boolean removeNull) {
		LinkedList<ItemIdentifierStack> list = new LinkedList<>();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).isEmpty()) {
				if (!removeNull) {
					list.add(null);
				}
			} else {
				list.add(ItemIdentifierStack.getFromStack(inv.getItem(i)));
			}
		}
		return list;
	}

	public static LinkedList<ItemIdentifierStack> getListSendQueue(LinkedList<Triplet<IRoutedItem, Direction, ItemSendMode>> _sendQueue) {
		LinkedList<ItemIdentifierStack> list = new LinkedList<>();
		for (Triplet<IRoutedItem, Direction, ItemSendMode> part : _sendQueue) {
			if (part == null) {
				list.add(null);
			} else {
				boolean added = false;
				for (ItemIdentifierStack stack : list) {
					if (stack.getItem().equals(part.getValue1().getItemIdentifierStack().getItem())) {
						stack.setStackSize(stack.getStackSize() + part.getValue1().getItemIdentifierStack().getStackSize());
						added = true;
						break;
					}
				}
				if (!added) {
					list.add(new ItemIdentifierStack(part.getValue1().getItemIdentifierStack()));
				}
			}
		}
		return list;
	}

	@Override
	public int compareTo(ItemIdentifierStack o) {
		int c = _item.compareTo(o._item);
		if (c == 0) {
			return getStackSize() - o.getStackSize();
		}
		return c;
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

}
