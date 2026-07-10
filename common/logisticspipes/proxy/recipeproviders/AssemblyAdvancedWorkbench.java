package logisticspipes.proxy.recipeproviders;/*
package logisticspipes.proxy.recipeproviders;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.silicon.TileAdvancedCraftingTable;

public class AssemblyAdvancedWorkbench implements ICraftingRecipeProvider {

	@Override
	public boolean canOpenGui(BlockEntity tile) {
		return (tile instanceof TileAdvancedCraftingTable);
	}

	@Override
	public boolean importRecipe(BlockEntity tile, ItemIdentifierInventory inventory) {
		if (!(tile instanceof TileAdvancedCraftingTable)) {
			return false;
		}

		TileAdvancedCraftingTable bench = (TileAdvancedCraftingTable) tile;
		ItemStack result = bench.getOutputSlot().getItem(0);

		if (result == null) {
			return false;
		}

		inventory.setItem(9, result);

		// Import
		for (int i = 0; i < bench.getCraftingSlots().getContainerSize(); i++) {
			if (i >= inventory.getContainerSize() - 2) {
				break;
			}
			final ItemStack newStack = bench.getCraftingSlots().getItem(i) == null ? null : bench.getCraftingSlots().getItem(i).copy();
			inventory.setItem(i, newStack);
		}

		// Compact
		for (int i = 0; i < inventory.getContainerSize() - 2; i++) {
			final ItemIdentifierStack stackInSlot = inventory.getIDStackInSlot(i);
			if (stackInSlot == null) {
				continue;
			}
			final ItemIdentifier itemInSlot = stackInSlot.getItem();
			for (int j = i + 1; j < inventory.getContainerSize() - 2; j++) {
				final ItemIdentifierStack stackInOtherSlot = inventory.getIDStackInSlot(j);
				if (stackInOtherSlot == null) {
					continue;
				}
				if (itemInSlot.equals(stackInOtherSlot.getItem())) {
					stackInSlot.setStackSize(stackInSlot.getStackSize() + stackInOtherSlot.getStackSize());
					inventory.setItem(i, stackInSlot);
					inventory.clearInventorySlotContents(j);
				}
			}
		}

		for (int i = 0; i < inventory.getContainerSize() - 2; i++) {
			if (inventory.getItem(i) != null) {
				continue;
			}
			for (int j = i + 1; j < inventory.getContainerSize() - 2; j++) {
				if (inventory.getItem(j) == null) {
					continue;
				}
				inventory.setItem(i, inventory.getIDStackInSlot(j));
				inventory.clearInventorySlotContents(j);
				break;
			}
		}
		return true;
	}
}
*/
