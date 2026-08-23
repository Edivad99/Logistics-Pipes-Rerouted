/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import logisticspipes.utils.transfer.ItemHandlers;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryUtil implements IInventoryUtil, ISpecialInsertion {

	protected final ResourceHandler<ItemResource> inventory;
	private final ProviderMode mode;

	public InventoryUtil(ResourceHandler<ItemResource> inventory, ProviderMode mode) {
		this.inventory = inventory;
		this.mode = mode;
	}

	@Override
	public int itemCount(ItemIdentifier item) {
		int count = 0;
		boolean first = true;
		for (int i = mode.getCropStart(); i < inventory.size() - mode.getCropEnd(); i++) {
			ItemStack stack = ItemUtil.getStack(inventory, i);
			if (stack.isEmpty() || !ItemIdentifier.get(stack).equals(item)) {
				continue;
			}
			if (first) {
				count = stack.getCount() - ((mode.getHideOnePerType() || mode.getHideOnePerStack()) ? 1 : 0);
				first = false;
			} else {
				count += stack.getCount() - (mode.getHideOnePerStack() ? 1 : 0);
			}
		}
		return count;
	}

	@Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() {
		Map<ItemIdentifier, Integer> items = new LinkedHashMap<>();
		for (int i = mode.getCropStart(); i < inventory.size() - mode.getCropEnd(); i++) {
			ItemStack stack = ItemUtil.getStack(inventory, i);
			if (stack.isEmpty()) {
				continue;
			}
			ItemIdentifier itemId = ItemIdentifier.get(stack);
			int stackSize = stack.getCount() - (mode.getHideOnePerStack() ? 1 : 0);
			Integer currentSize = items.get(itemId);
			if (currentSize == null) {
				items.put(itemId, stackSize - (mode.getHideOnePerType() ? 1 : 0));
			} else {
				items.put(itemId, currentSize + stackSize);
			}
		}
		return items;
	}

	@Override
    public Set<ItemIdentifier> getItems() {
		Set<ItemIdentifier> items = new TreeSet<>();
		for (int i = mode.getCropStart(); i < inventory.size() - mode.getCropEnd(); i++) {
			ItemStack stack = ItemUtil.getStack(inventory, i);
			if (stack.isEmpty()) {
				continue;
			}
			items.add(ItemIdentifier.get(stack));
		}
		return items;
	}

	@Override
    public ItemStack getSingleItem(ItemIdentifier item) {
		return getMultipleItems(item, 1);
	}

	@Override
    public ItemStack getMultipleItems(ItemIdentifier item, int count) {
		if (itemCount(item) < count) {
			return ItemStack.EMPTY;
		}
		ItemStack outputStack = ItemStack.EMPTY;
		boolean first = true;

		for (int i = mode.getCropStart(); i < inventory.size() - mode.getCropEnd() && count > 0; i++) {
			ItemStack stack = ItemUtil.getStack(inventory, i);
			if (stack.isEmpty() || (stack.getCount() == 1 && mode.getHideOnePerStack()) || !ItemIdentifier.get(stack).equals(item)) {
				continue;
			}
			int itemsToSplit = Math.min(count, stack.getCount() - (((first && mode.getHideOnePerType()) || mode.getHideOnePerStack()) ? 1 : 0));
			first = false;
			if (itemsToSplit == 0) {
				continue;
			}
			ItemStack removed = ItemHandlers.extractItem(inventory, i, itemsToSplit, false);
			if (outputStack.isEmpty()) {
				outputStack = removed;
			} else {
				outputStack.setCount(outputStack.getCount() + removed.getCount());
			}
			count -= removed.getCount();
		}
		return outputStack;
	}

	//Ignores slot/item hiding
	@Override
	public boolean containsUndamagedItem(ItemIdentifier item) {
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = ItemUtil.getStack(inventory, i);
			if (stack.isEmpty()) {
				continue;
			}
			if (ItemIdentifier.get(stack).getUndamaged().equals(item)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int roomForItem(ItemStack stack) {
		// Special casing for "unlimited" storage items
		if (inventory.size() == 1 && ItemHandlers.slotLimit(inventory, 0) == Integer.MAX_VALUE) {
			ItemStack content = ItemHandlers.extractItem(inventory, 0, Integer.MAX_VALUE, true);
			if (content.isEmpty()) {
				return Integer.MAX_VALUE;
			}
			return Integer.MAX_VALUE - content.getCount();
		}

		int totalRoom = 0;
		for (int i = 0; i < inventory.size() && stack.getCount() > totalRoom; i++) {
			// stack.copy() because other TileEntities might modify stack.
			// "This must not be modified by the item handler." lol
			ItemStack leftover = ItemUtil.insertItemReturnRemaining(inventory, i, stack.copy(), true, null);
			totalRoom += stack.getCount() - leftover.getCount();
		}
		return totalRoom;
	}

	@Override
	public int getContainerSize() {
		return inventory.size();
	}

	@Override
    public ItemStack getItem(int i) {
		return ItemUtil.getStack(inventory, i);
	}

	@Override
    public ItemStack removeItem(int i, int j) {
		return ItemHandlers.extractItem(inventory, i, j, false);
	}

	@Override
	public int addToSlot(ItemStack stack, int slot) {
		int wanted = stack.getCount();
		ItemStack rest = ItemUtil.insertItemReturnRemaining(inventory, slot, stack.copy(), false, null);
		return wanted - rest.getCount();
	}
}
