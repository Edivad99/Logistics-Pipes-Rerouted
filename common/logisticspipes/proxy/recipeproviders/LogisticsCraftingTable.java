package logisticspipes.proxy.recipeproviders;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.proxy.interfaces.IFuzzyRecipeProvider;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.inventory.FuzzySlotAccess;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.SlotAccess;
import network.rs485.logisticspipes.property.BitSetProperty;

public class LogisticsCraftingTable implements IFuzzyRecipeProvider {

	@Override
	public boolean canOpenGui(BlockEntity tile) {
		return (tile instanceof LogisticsCraftingTableTileEntity);
	}

	@Override
	public boolean importRecipe(BlockEntity tile, IItemIdentifierInventory inventory) {
		if (!(tile instanceof LogisticsCraftingTableTileEntity)) {
			return false;
		}

		LogisticsCraftingTableTileEntity bench = (LogisticsCraftingTableTileEntity) tile;
		ItemIdentifierStack result = bench.resultInv.getIDStackInSlot(0);

		if (result == null) {
			return false;
		}

		inventory.setItem(9, result);

		// Import
		for (int i = 0; i < bench.matrix.getContainerSize(); i++) {
			if (i >= inventory.getContainerSize() - 2) {
				break;
			}
			ItemStack stackInSlot = bench.matrix.getItem(i);
			if (!stackInSlot.isEmpty() && stackInSlot.getCount() > 1) {
				stackInSlot = stackInSlot.copy();
				stackInSlot.setCount(1);
			}
			inventory.setItem(i, stackInSlot);
		}

		if (!bench.isFuzzy()) {
			inventory.getSlotAccess().compactFirst(9);
		}

		return true;
	}

	@Override
	public void importFuzzyFlags(BlockEntity tile, SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
		if (!(tile instanceof LogisticsCraftingTableTileEntity)) {
			return;
		}

		LogisticsCraftingTableTileEntity bench = (LogisticsCraftingTableTileEntity) tile;

		if (!bench.isFuzzy()) {
			return;
		}

		fuzzyFlags.replaceWith(bench.fuzzyFlags);
		new FuzzySlotAccess(slotAccess, fuzzyFlags).compactFirst(9);
	}

}
