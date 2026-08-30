/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import logisticspipes.LogisticsPipes;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.ItemStackLoader;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.tuples.Pair;

// Container became Iterable<ItemStack> in 1.21.5, so this can no longer also declare
// Iterable over its own pair type: the two Iterable parameterisations conflict. The pair
// iteration is reached through contents() instead.
public class SimpleStackInventory implements Container, ValueIOSerializable {

	private static final Component TEXT_COMPONENT_EMPTY = Component.literal("");

	private final NonNullList<ItemStack> stackList;
	private final String name;
	private final int stackLimit;

	private final LinkedList<ISimpleInventoryEventHandler> listener = new LinkedList<>();

	public SimpleStackInventory(SimpleStackInventory copy) {
		this(copy.getContainerSize(), copy.name, copy.stackLimit);
		for (int i = 0; i < copy.getContainerSize(); i++) {
			stackList.set(i, copy.getItem(i).copy());
		}
	}

	public SimpleStackInventory(int size, String name, int stackLimit) {
		stackList = NonNullList.withSize(size, ItemStack.EMPTY);
		this.name = name;
		this.stackLimit = stackLimit;
	}

	@Override
	public int getContainerSize() {
		return stackList.size();
	}

	@Override
	public boolean isEmpty() {
		return stackList.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int i) {
		return stackList.get(i);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		final ItemStack stack = stackList.get(slot);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (stack.getCount() > count) {
			ItemStack ret = stack.copy();
			ret.setCount(count);
			stack.setCount(stack.getCount() - count);
			return ret;
		}
		return stackList.set(slot, ItemStack.EMPTY);
	}

	@Override
	public void setItem(int slot, ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			stackList.set(slot, ItemStack.EMPTY);
		} else {
			stackList.set(slot, itemstack.copy());
		}
	}

	public String getName() {
		return name;
	}

	public Component getDisplayName() {
		return TEXT_COMPONENT_EMPTY;
	}

	@Override
	public int getMaxStackSize() {
		return stackLimit;
	}

	@Override
	public void setChanged() {
		for (ISimpleInventoryEventHandler handler : listener) {
			handler.InventoryChanged(this);
		}
	}

	@Override
	public boolean stillValid(Player entityplayer) {
		return false;
	}

    @Override
    public void startOpen(ContainerUser containerUser) {}

    @Override
	public void stopOpen(ContainerUser containerUser) {}

	@Override
	public void deserialize(ValueInput input) {
		deserialize(input, "");
	}

	public void deserialize(ValueInput input, String prefix) {
		// Each entry carries its own slot index, so the list stays sparse and order-independent --
		// which is why a ValueInputList being iterable but not indexable costs nothing here.
		for (ValueInput entry : input.childrenListOrEmpty(prefix + "items")) {
			int index = entry.getIntOr("index", 0);
			if (index < stackList.size()) {
				stackList.set(index, ItemStackLoader.loadItemStack(entry));
			} else {
				LogisticsPipes.LOG.error("SimpleInventory: java.lang.ArrayIndexOutOfBoundsException: " + index + " of " + stackList.size());
			}
		}
	}

	@Override
	public void serialize(ValueOutput output) {
		serialize(output, "");
	}

	public void serialize(ValueOutput output, String prefix) {
		ValueOutput.ValueOutputList list = output.childrenList(prefix + "items");
		for (int i = 0; i < stackList.size(); ++i) {
			ItemStack stack = stackList.get(i);
			if (!stack.isEmpty()) {
				ValueOutput entry = list.addChild();
				entry.putInt("index", i);
				ItemStackLoader.saveItemStack(entry, stack);
			}
		}
		output.putInt(prefix + "itemsCount", stackList.size());
	}

	public void dropContents(Level level, BlockPos pos) {
		if (MainProxy.isServer(level)) {
			for (int i = 0; i < stackList.size(); i++) {
				dropSlot(i, level, pos);
			}
		}
	}

	private void dropSlot(int slot, Level level, BlockPos pos) {
		final ItemStack slotStack = stackList.get(slot);
		IntStream.range(0, (slotStack.getCount() / slotStack.getMaxStackSize()) + 1)
				.mapToObj(i -> removeItem(slot, slotStack.getMaxStackSize()))
				.filter(dropStack -> !dropStack.isEmpty())
				.forEach(dropStack -> {
					float f1 = 0.7F;
					double d = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					double d1 = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					double d2 = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					ItemEntity entityitem = new ItemEntity(level, pos.getX() + d, pos.getY() + d1, pos.getZ() + d2, dropStack);
					entityitem.setDefaultPickUpDelay();
					level.addFreshEntity(entityitem);
				});
	}

	public void addListener(ISimpleInventoryEventHandler listner) {
		if (!listener.contains(listner)) {
			listener.add(listner);
		}
	}

	public void removeListener(ISimpleInventoryEventHandler listner) {
		listener.remove(listner);
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		return stackList.set(i, ItemStack.EMPTY);
	}

	private int tryAddToSlot(int i, ItemStack stack, int realstacklimit) {
		ItemStack slotStack = stackList.get(i);
		if (slotStack.isEmpty()) {
			final ItemStack copy = stack.copy();
			stackList.set(i, copy);
			copy.setCount(Math.min(copy.getCount(), realstacklimit));
			return copy.getCount();
		}
		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		ItemIdentifier slotIdent = ItemIdentifier.get(slotStack);
		if (slotIdent.equals(stackIdent)) {
			slotStack.setCount(slotStack.getCount() + stack.getCount());
			if (slotStack.getCount() > realstacklimit) {
				int ans = stack.getCount() - (slotStack.getCount() - realstacklimit);
				slotStack.setCount(realstacklimit);
				return ans;
			} else {
				return stack.getCount();
			}
		} else {
			return 0;
		}
	}

	public int addCompressed(ItemStack stack, boolean ignoreMaxStackSize) {
		if (stack.isEmpty()) return 0;
		stack = stack.copy();

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		int stacklimit = stackLimit;
		if (!ignoreMaxStackSize) {
			stacklimit = Math.min(stacklimit, stackIdent.getMaxStackSize());
		}

		for (int i = 0; i < stackList.size(); i++) {
			if (stack.getCount() <= 0) {
				break;
			}
			if (stackList.get(i).isEmpty()) {
				continue; //Skip Empty Slots on first attempt.
			}
			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}
		for (int i = 0; i < stackList.size(); i++) {
			if (stack.getCount() <= 0) {
				break;
			}
			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}
		setChanged();
		return stack.getCount();
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return true;
	}

	@kotlin.Deprecated(message = "not implemented")
	public int getField(int id) {
		return 0;
	}

	@kotlin.Deprecated(message = "not implemented")
	public void setField(int id, int value) {}

	@kotlin.Deprecated(message = "not implemented")
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clearContent() {
		Collections.fill(stackList, ItemStack.EMPTY);
	}

	public void clearInventorySlotContents(int i) {
		stackList.set(i, ItemStack.EMPTY);
	}

	public boolean hasCustomName() {
		return true;
	}

	/** Pair-wise view of the contents; was the class's own iterator() before 1.21.5. */
	public Iterable<Pair<ItemStack, Integer>> contents() {
		return this::pairIterator;
	}

	private Iterator<Pair<ItemStack, Integer>> pairIterator() {
		final Iterator<ItemStack> iter = stackList.iterator();
		return new Iterator<Pair<ItemStack, Integer>>() {

			int pos = -1;

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Pair<ItemStack, Integer> next() {
				pos++;
				return new Pair<>(iter.next(), pos);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Returns a stream over all non-empty item stacks in this inventory.
	 */
	public Stream<ItemStack> stackStream() {
		return stackList.stream().filter(itemStack -> !itemStack.isEmpty());
	}

}
