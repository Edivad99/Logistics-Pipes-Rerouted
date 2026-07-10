package logisticspipes.asm.wrapper;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

class CraftingRecipeProviderWrapper extends AbstractWrapper implements ICraftingRecipeProvider {

	private ICraftingRecipeProvider provider;
	private final String name;

	CraftingRecipeProviderWrapper(ICraftingRecipeProvider provider, String name) {
		this.provider = provider;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getTypeName() {
		return "RecipeProvider";
	}

	@Override
	public boolean canOpenGui(BlockEntity tile) {
		if (isEnabled()) {
			try {
				return provider.canOpenGui(tile);
			} catch (Exception | NoClassDefFoundError e) {
				handleException(e);
			}
		}
		return false;
	}

	@Override
	public boolean importRecipe(BlockEntity tile, IItemIdentifierInventory inventory) {
		if (isEnabled()) {
			try {
				return provider.importRecipe(tile, inventory);
			} catch (Exception | NoClassDefFoundError e) {
				handleException(e);
			}
		}
		return false;
	}
}
