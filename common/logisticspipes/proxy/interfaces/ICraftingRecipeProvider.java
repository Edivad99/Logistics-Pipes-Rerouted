package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

public interface ICraftingRecipeProvider {

	boolean canOpenGui(BlockEntity tile);

	boolean importRecipe(BlockEntity tile, IItemIdentifierInventory inventory);

}
