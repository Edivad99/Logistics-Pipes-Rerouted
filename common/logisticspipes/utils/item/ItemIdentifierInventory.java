/*
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import logisticspipes.LogisticsPipes;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import network.rs485.logisticspipes.IStore;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.SlotAccess;
import network.rs485.logisticspipes.util.items.ItemStackLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Container became Iterable<ItemStack> in 1.21.5, so this can no longer also declare
// Iterable over its own pair type: the two Iterable parameterisations conflict. The pair
// iteration is reached through contents() instead.
public class ItemIdentifierInventory
		implements IStore, IItemIdentifierInventory {

	private final Object[] ccTypeHolder = new Object[1];
	private final ItemIdentifierStack[] contents;
	private final String name;
	private final int stackLimit;
	private final HashMap<ItemIdentifier, Integer> contentsMap;
	private final HashSet<ItemIdentifier> contentsUndamagedSet;
	private final HashSet<ItemIdentifier> contentsNoNBTSet;
	private final HashSet<ItemIdentifier> contentsUndamagedNoNBTSet;
	private final boolean isLiquidInventory;

	private final LinkedList<ISimpleInventoryEventHandler> listener = new LinkedList<>();

	public final SlotAccess slotAccess = new SlotAccess() {

		@Override
		public void mergeSlots(int intoSlot, int fromSlot) {
			if (contents[intoSlot] == null) {
				contents[intoSlot] = contents[fromSlot];
			} else {
				contents[intoSlot].setStackSize(contents[intoSlot].getStackSize() + contents[fromSlot].getStackSize());
			}
			contents[fromSlot] = null;
			updateContents();
		}

		@Override
		public boolean canMerge(int intoSlot, int fromSlot) {
			return contents[intoSlot].getItem().equals(contents[fromSlot].getItem());
		}

		@Override
		public boolean isSlotEmpty(int idx) {
			return contents[idx] == null;
		}

	};

	public ItemIdentifierInventory(int size, String name, int stackLimit, boolean liquidInv) {
		contents = new ItemIdentifierStack[size];
		this.name = name;
		this.stackLimit = stackLimit;
		contentsMap = new HashMap<>((int) (size * 1.5));
		contentsUndamagedSet = new HashSet<>((int) (size * 1.5));
		contentsNoNBTSet = new HashSet<>((int) (size * 1.5));
		contentsUndamagedNoNBTSet = new HashSet<>((int) (size * 1.5));
		isLiquidInventory = liquidInv;
	}

	public ItemIdentifierInventory(int size, String name, int stackLimit) {
		this(size, name, stackLimit, false);
	}

	public ItemIdentifierInventory(ItemIdentifierInventory copy) {
		contents = Arrays.copyOf(copy.contents, copy.contents.length);
		for (int i = 0; i < contents.length; i++) {
			if (copy.contents[i] != null) contents[i] = new ItemIdentifierStack(copy.contents[i]);
		}
		name = copy.name;
		stackLimit = copy.stackLimit;
		contentsMap = new HashMap<>(copy.contentsMap);
		contentsUndamagedSet = new HashSet<>(copy.contentsUndamagedSet);
		contentsNoNBTSet = new HashSet<>(copy.contentsNoNBTSet);
		contentsUndamagedNoNBTSet = new HashSet<>(copy.contentsUndamagedNoNBTSet);
		isLiquidInventory = copy.isLiquidInventory;
	}

	public static void dropItems(Level level, ItemStack stack, BlockPos pos) {
		dropItems(level, stack, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void dropItems(Level level, ItemStack stack, int i, int j, int k) {
		if (stack.isEmpty()) return;
		float f1 = 0.7F;
		double d = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		double d1 = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		double d2 = (level.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		ItemEntity entityitem = new ItemEntity(level, i + d, j + d1, k + d2, stack);
		entityitem.setPickUpDelay(10);
		level.addFreshEntity(entityitem);
	}

	@Override
	public int getContainerSize() {
		return contents.length;
	}

	@Override
    public ItemStack getItem(int i) {
		if (contents[i] == null) {
			return ItemStack.EMPTY;
		}
		return contents[i].makeNormalStack();
	}

	@Override
    @Nullable
	public ItemIdentifierStack getIDStackInSlot(int i) {
		return contents[i];
	}

	@Override
    public ItemStack removeItem(int slot, int count) {
		if (contents[slot] == null) {
			return ItemStack.EMPTY;
		}
		ItemStack ret = contents[slot].makeNormalStack();
		if (contents[slot].getStackSize() > count) {
			ret.setCount(count);
			contents[slot].setStackSize(contents[slot].getStackSize() - count);
		} else {
			contents[slot] = null;
		}
		updateContents();
		return ret;
	}

	@Override
	public void setItem(int i, ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			contents[i] = null;
		} else {
			if (isInvalidStack(itemstack)) {
				if (LogisticsPipes.isDEBUG()) {
					new UnsupportedOperationException("Not valid for this Inventory: (" + itemstack + ")")
							.printStackTrace();
				}
				return;
			}
			contents[i] = ItemIdentifierStack.getFromStack(itemstack);
		}
		updateContents();
	}

	@Override
	public void setItem(int i, @Nullable ItemIdentifierStack itemstack) {
		if (itemstack == null) {
			contents[i] = null;
		} else {
			if (!isValidStack(itemstack)) {
				if (LogisticsPipes.isDEBUG()) {
					new UnsupportedOperationException("Not valid for this Inventory: (" + itemstack + ")")
							.printStackTrace();
				}
				return;
			}
			contents[i] = itemstack;
		}
		updateContents();
	}

	@Override
	public int getMaxStackSize() {
		return stackLimit;
	}

	@Override
	public void setChanged() {
		updateContents();
		for (ISimpleInventoryEventHandler handler : listener) {
			handler.InventoryChanged(this);
		}
	}

	@Override
	public boolean stillValid(Player entityplayer) {
		return true;
	}

	@Override
	public void startOpen(Player player) {}

	@Override
	public void stopOpen(Player player) {}

	@Override
	public void readFromNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		readFromNBT(tag, provider, "");
	}

	public void readFromNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider, String prefix) {
		ListTag listtag = tag.getListOrEmpty(prefix + "items");

		Arrays.fill(contents, null);
		for (int j = 0; j < listtag.size(); ++j) {
			CompoundTag compoundTag = listtag.getCompoundOrEmpty(j);
			int index = compoundTag.getIntOr("index", 0);
			if (index >= 0 && index < contents.length) {
				ItemStack stack = ItemStackLoader.loadAndFixItemStackFromNBT(compoundTag, provider);
				ItemIdentifierStack itemstack = ItemIdentifierStack.getFromStack(stack);
				if (isValidStack(itemstack)) {
					contents[index] = itemstack;
				}
			} else {
				LogisticsPipes.LOG.error("SimpleInventory: java.lang.ArrayIndexOutOfBoundsException: " + index + " of "
						+ contents.length);
			}
		}
		updateContents();
	}

	@Override
	public void writeToNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		writeToNBT(tag, provider, "");
	}

	public void writeToNBT(CompoundTag tag, HolderLookup.Provider provider, String prefix) {
		ListTag listTag = new ListTag();
		for (int i = 0; i < contents.length; ++i) {
			if (contents[i] != null && contents[i].getStackSize() > 0) {
				CompoundTag stackTag = new CompoundTag();
				stackTag.putInt("index", i);
				listTag.add(contents[i].makeNormalStack().save(provider, stackTag));
			}
		}
		tag.put(prefix + "items", listTag);
		tag.putInt(prefix + "itemsCount", contents.length);
	}

	public void dropContents(Level level, BlockPos pos) {
		dropContents(level, pos.getX(), pos.getY(), pos.getZ());
	}

	public void dropContents(Level level, int posX, int posY, int posZ) {
		if (MainProxy.isServer(level)) {
			for (int i = 0; i < contents.length; i++) {
				while (contents[i] != null) {
					ItemStack todrop = removeItem(i, contents[i].getItem().getMaxStackSize());
					ItemIdentifierInventory.dropItems(level, todrop, posX, posY, posZ);
				}
			}
			updateContents();
		}
	}

	@Override
	public void addListener(ISimpleInventoryEventHandler listener) {
		if (!this.listener.contains(listener)) {
			this.listener.add(listener);
		}
	}

	@Override
	public void removeListener(ISimpleInventoryEventHandler listener) {
		this.listener.remove(listener);
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		if (contents[i] == null) {
			return ItemStack.EMPTY;
		}

		ItemStack stackToTake = contents[i].makeNormalStack();
		contents[i] = null;
		updateContents();
		return stackToTake;
	}

	@Override
	public void handleItemIdentifierList(Collection<ItemIdentifierStack> allItems) {
		int i = 0;
		for (ItemIdentifierStack stack : allItems) {
			if (contents.length <= i) {
				break;
			}
			contents[i] = stack;
			i++;
		}
		setChanged();
	}

	private int tryAddToSlot(int i, ItemStack stack, int realstacklimit) {
		if (isInvalidStack(stack)) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Not valid for this Inventory: (" + stack + ")").printStackTrace();
			}
			return 0;
		}
		ItemIdentifierStack slot = contents[i];

		if (slot == null) {
			contents[i] = ItemIdentifierStack.getFromStack(stack);
			contents[i].setStackSize(Math.min(contents[i].getStackSize(), realstacklimit));
			return contents[i].getStackSize();
		}

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		ItemIdentifier slotIdent = slot.getItem();

		if (slotIdent.equals(stackIdent)) {
			slot.setStackSize(slot.getStackSize() + stack.getCount());

			if (slot.getStackSize() > realstacklimit) {
				int ans = stack.getCount() - (slot.getStackSize() - realstacklimit);
				slot.setStackSize(realstacklimit);
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

		if (isInvalidStack(stack)) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Not valid for this Inventory: (" + stack + ")").printStackTrace();
			}
			return stack.getCount();
		}

		stack = stack.copy();

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		int stacklimit = stackLimit;

		if (!ignoreMaxStackSize) {
			stacklimit = Math.min(stacklimit, stackIdent.getMaxStackSize());
		}

		for (int i = 0; i < contents.length; i++) {
			if (stack.getCount() <= 0) break;
			if (contents[i] == null) continue; //Skip Empty Slots on first attempt.

			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}

		for (int i = 0; i < contents.length; i++) {
			if (stack.getCount() <= 0) break;

			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}

		setChanged();
		return stack.getCount();
	}

	/* InventoryUtil-like functions */

	/**
	 * Rebuilds the four lookup indexes. Each one stores a <i>projected</i> identifier, so callers
	 * have to project their query the same way -- see the contains* methods below.
	 * <p>
	 * The composition order below is load-bearing and must not be "tidied up". The projections do
	 * not commute in one edge case: {@code getIgnoringNBT()} drops UNBREAKABLE, so for an item whose
	 * patch sets it, the projection is damageable while the original is not, and
	 * {@code getIgnoringNBT().getUndamaged()} differs from {@code getUndamaged().getIgnoringNBT()}.
	 * Every set/query pair therefore has to agree on the order, and the ones here match what
	 * PipeItemsFirewall and the sink modules use.
	 */
	private void updateContents() {
		contentsMap.clear();
		contentsUndamagedSet.clear();
		contentsNoNBTSet.clear();
		contentsUndamagedNoNBTSet.clear();
		for (ItemIdentifierStack content : contents) {
			if (content == null) continue;

			ItemIdentifier itemId = content.getItem();
			contentsMap.merge(itemId, content.getStackSize(), Integer::sum);
			contentsUndamagedSet.add(itemId
					.getUndamaged()); // add is cheaper than check then add; it just returns false if it is already there
			contentsNoNBTSet.add(itemId
					.getIgnoringNBT()); // add is cheaper than check then add; it just returns false if it is already there
			contentsUndamagedNoNBTSet.add(itemId.getIgnoringNBT()
					.getUndamaged()); // add is cheaper than check then add; it just returns false if it is already there
		}
	}

	@Override
	public int itemCount(final ItemIdentifier item) {
		return contentsMap.getOrDefault(item, 0);
	}

	@Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() {
		return contentsMap;
	}

	@Override
	public boolean containsItem(final ItemIdentifier item) {
		return contentsMap.containsKey(item);
	}

	@Override
	public boolean containsUndamagedItem(final ItemIdentifier item) {
		return contentsUndamagedSet.contains(item);
	}

	@Override
	public boolean containsExcludeNBTItem(final ItemIdentifier item) {
		return contentsNoNBTSet.contains(item);
	}

	@Override
	public boolean containsUndamagedExcludeNBTItem(final ItemIdentifier item) {
		return contentsUndamagedNoNBTSet.contains(item);
	}

	@Override
	public boolean isEmpty() {
		return contentsMap.isEmpty();
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return true;
	}

    @Override
	public void clearContent() {
		clear();
	}

	public void clear() {
		Arrays.fill(contents, null);
		updateContents();
	}

	@Override
	public void clearInventorySlotContents(int i) {
		contents[i] = null;
		updateContents();
	}

	@Override
	public void recheckStackLimit() {
		for (ItemIdentifierStack content : contents) {
			if (content != null) {
				content.setStackSize(Math.min(content.getStackSize(), stackLimit));
			}
		}
	}

	private boolean isInvalidStack(ItemStack stack) {
		if (isLiquidInventory && !stack.isEmpty()) {
			return FluidIdentifier.get(stack) == null;
		}
		return false;
	}

	private boolean isValidStack(@Nullable ItemIdentifierStack stack) {
		if (stack == null) {
            return true;
        }
		if (isLiquidInventory) {
			return FluidIdentifier.get(stack.getItem()) != null;
		}
		return true;
	}

    private Iterator<Pair<ItemIdentifierStack, Integer>> pairIterator() {
		final Iterator<ItemIdentifierStack> iter = Arrays.asList(contents).iterator();
		return new Iterator<>() {

            int pos = -1;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<ItemIdentifierStack, Integer> next() {
                pos++;
                return new Pair<>(iter.next(), pos);
            }
        };
	}

	public void clearGrid() {
		for (int i = 0; i < getContainerSize(); i++) {
			contents[i] = null;
		}
		updateContents();
	}

	public String getName() {
		return name;
	}

	public NonNullList<ItemStack> toNonNullList() {
		NonNullList<ItemStack> list = NonNullList.create();
		list.addAll(0, Arrays.stream(contents)
				.filter(Objects::nonNull)
				.map(ItemIdentifierStack::makeNormalStack)
				.toList());
		return list;
	}

	@Override
	public List<String> getClientInformation() {
		return Arrays.stream(contents).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

	@Override
	public Iterable<Pair<ItemIdentifierStack, Integer>> contents() {
		return this::pairIterator;
	}

	@Override
	public SlotAccess getSlotAccess() {
		return slotAccess;
	}

}
