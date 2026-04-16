package logisticspipes.recipes;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import logisticspipes.LPItems;
import logisticspipes.items.ItemModule;
import logisticspipes.modules.LogisticsModule;

public class CraftingRecipes implements IRecipeProvider {

	@Override
	public void loadRecipes() {
		registerResetRecipes();
	}

	private void registerResetRecipes() {
		for (ResourceLocation moduleResource : LPItems.modules.values()) {
			final Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(moduleResource);
			if (item instanceof ItemModule) {
				LogisticsModule module = ((ItemModule) item).getModuleForItem(new ItemStack(item), null, null, null);
				if (module == null) continue;
				CompoundTag tag = new CompoundTag();
				module.writeToNBT(tag);
				if (!tag.isEmpty()) {
					RecipeManager.craftingManager.addShapelessResetRecipe(item, 0);
				}
			}
		}

		// The 1.12.2 remote orderer used damage values 1..16 for 16 dye-coloured variants, plus
		// matching dye recipes. Damage-as-variant was removed in 1.20.1: items are identified by
		// id alone. Until/unless the coloured orderer is reintroduced as NBT-tagged state or as
		// 16 separate items, there is only one canonical remote orderer — and a single reset
		// recipe is enough.
		RecipeManager.craftingManager.addShapelessResetRecipe(LPItems.remoteOrderer.get(), 0);
	}
}
