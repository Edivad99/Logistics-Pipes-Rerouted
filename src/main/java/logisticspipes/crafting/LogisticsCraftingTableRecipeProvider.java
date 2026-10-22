package logisticspipes.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.inventory.FuzzySlotAccess;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

public class LogisticsCraftingTableRecipeProvider implements ICraftingRecipeProvider {

    @Override
    public boolean canOpenScreen(BlockEntity blockEntity) {
        return blockEntity instanceof LogisticsCraftingTableBlockEntity;
    }

    @Override
    public boolean importRecipe(BlockEntity blockEntity, IItemIdentifierInventory inventory) {
        if (!(blockEntity instanceof LogisticsCraftingTableBlockEntity craftingTableBlockEntity)) {
            return false;
        }

        ItemIdentifierStack result = craftingTableBlockEntity.resultInv.getIDStackInSlot(0);
        if (result == null) {
            return false;
        }
        inventory.setItem(9, result);

        // Import
        for (int i = 0; i < craftingTableBlockEntity.matrix.getContainerSize(); i++) {
            if (i >= inventory.getContainerSize() - 2) {
                break;
            }
            ItemStack stackInSlot = craftingTableBlockEntity.matrix.getItem(i);
            if (!stackInSlot.isEmpty() && stackInSlot.getCount() > 1) {
                stackInSlot = stackInSlot.copy();
                stackInSlot.setCount(1);
            }
            inventory.setItem(i, stackInSlot);
        }
        if (!craftingTableBlockEntity.isFuzzy()) {
            inventory.getSlotAccess().compactFirst(9);
        }
        return true;
    }

    @Override
    public void importFuzzyFlags(BlockEntity blockEntity, SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
        if (!(blockEntity instanceof LogisticsCraftingTableBlockEntity craftingTableBlockEntity)) {
            return;
        }
        if (!craftingTableBlockEntity.isFuzzy()) {
            return;
        }
        fuzzyFlags.replaceWith(craftingTableBlockEntity.fuzzyFlags);
        new FuzzySlotAccess(slotAccess, fuzzyFlags).compactFirst(9);
    }
}
