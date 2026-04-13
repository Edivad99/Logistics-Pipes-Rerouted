package com.enderio.core.common.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

public class InventoryWrapper implements WorldlyContainer {

	public static WorldlyContainer asSidedInventory(Container inv) {
		if (inv == null) {
			return null;
		}
		if (inv instanceof WorldlyContainer) {
			return (WorldlyContainer) inv;
		}
		return new InventoryWrapper(inv);
	}

	public InventoryWrapper(Container inventory) {
	}

	public Container getWrappedInv() {
		return null;
	}

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	@Nonnull
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	@Nonnull
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, @Nullable ItemStack itemStack) {

	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(@Nonnull Player player) {
		return false;
	}

	@Override
	public boolean canPlaceItem(int slot, @Nonnull ItemStack itemStack) {
		return false;
	}

	@Override
	@Nonnull
	public int[] getSlotsForFace(@Nonnull Direction side) {
		return new int[0];
	}

	@Override
	public boolean canInsertItem(int slot, @Nonnull ItemStack itemStack, @Nonnull Direction side) {
		return canPlaceItem(slot, itemStack);
	}

	@Override
	public boolean canExtractItem(int slot, @Nonnull ItemStack itemStack, @Nonnull Direction side) {
		return slot >= 0 && slot < getContainerSize();
	}

	@Override
	@Nonnull
	public ItemStack removeItemNoUpdate(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void startOpen(@Nonnull Player player) {
	}

	@Override
	public void stopOpen(@Nonnull Player player) {
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
	}

	@Override
	@Nonnull
	public String getName() {
		return "";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	@Nonnull
	public Component getDisplayName() {
		return Component.literal("");
	}

}