package logisticspipes.proxy.buildcraft.recipeprovider;
// TODO: BuildCraft not ported to 1.20.1 — stub

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

public class AssemblyTable implements ICraftingRecipeProvider {
    @Override public boolean canOpenGui(BlockEntity tile) { return false; }
    @Override public boolean importRecipe(BlockEntity tile, IItemIdentifierInventory inventory) { return false; }
}
