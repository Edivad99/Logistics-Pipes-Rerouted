package logisticspipes.crafting;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;

public interface ICraftingRecipeProvider {

    boolean canOpenScreen(BlockEntity blockEntity);

    boolean importRecipe(BlockEntity blockEntity, IItemIdentifierInventory inventory);

    void importFuzzyFlags(BlockEntity blockEntity, SlotAccess slotAccess, BitSetProperty fuzzyFlags);

}
