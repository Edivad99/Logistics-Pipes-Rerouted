package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.inventory.SlotAccess;

public interface IFuzzyRecipeProvider extends ICraftingRecipeProvider {

	void importFuzzyFlags(BlockEntity tile, SlotAccess slotAccess, BitSetProperty fuzzyFlags);

}
