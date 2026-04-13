package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import network.rs485.logisticspipes.inventory.SlotAccess;
import network.rs485.logisticspipes.property.BitSetProperty;

public interface IFuzzyRecipeProvider extends ICraftingRecipeProvider {

	void importFuzzyFlags(BlockEntity tile, SlotAccess slotAccess, BitSetProperty fuzzyFlags);

}
