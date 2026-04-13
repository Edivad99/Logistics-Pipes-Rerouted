/*
package logisticspipes.proxy.recipeproviders;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.item.ItemIdentifierInventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.factory.TileAutoWorkbench;

public class AutoWorkbench implements ICraftingRecipeProvider {

	@Override
	public boolean canOpenGui(BlockEntity tile) {
		return (tile instanceof TileAutoWorkbench);
	}

	@Override
	public boolean importRecipe(BlockEntity tile, ItemIdentifierInventory inventory) {
		if (!(tile instanceof TileAutoWorkbench)) {
			return false;
		}

		TileAutoWorkbench bench = (TileAutoWorkbench) tile;
		ItemStack result = bench.craftMatrix.getRecipeOutput();
		//ItemStack result = bench.getItem(TileAutoWorkbench.SLOT_RESULT);

		if (result == null) {
			return false;
		}

		inventory.setItem(9, result);

		// Import
		for (int i = 0; i < bench.craftMatrix.getContainerSize(); i++) {
			if (i >= inventory.getContainerSize() - 2) {
				break;
			}
			final ItemStack newStack = bench.craftMatrix.getItem(i).copy();
			if (!newStack.isEmpty() && newStack.getCount() > 1) {
				newStack.getCount() = 1;
			}
			inventory.setItem(i, newStack);
		}

		inventory.compactFirst(9);

		return true;
	}
}
*/