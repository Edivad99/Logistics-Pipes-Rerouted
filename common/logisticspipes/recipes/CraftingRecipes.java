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
		// @formatter:off
		String[] dyes = {
			"dyeBlack",
			"dyeRed",
			"dyeGreen",
			"dyeBrown",
			"dyeBlue",
			"dyePurple",
			"dyeCyan",
			"dyeLightGray",
			"dyeGray",
			"dyePink",
			"dyeLime",
			"dyeYellow",
			"dyeLightBlue",
			"dyeMagenta",
			"dyeOrange",
			"dyeWhite"
		};
		// @formatter:on

		registerResetRecipe(dyes);
	}

	private void registerResetRecipe(String[] dyes) {
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

		for (int i = 1; i < 17; i++) {
			// new ItemStack(item, count, damage) removed in 1.20.1 — addOrdererRecipe TODO: migrate damage to NBT or separate items
			// RecipeManager.craftingManager.addOrdererRecipe(new ItemStack(LPItems.remoteOrderer.get(), 1, i), dyes[i - 1], new ItemStack(LPItems.remoteOrderer.get(), 1, -1));
			RecipeManager.craftingManager.addShapelessResetRecipe(LPItems.remoteOrderer.get(), i);
		}
		RecipeManager.craftingManager.addShapelessResetRecipe(LPItems.remoteOrderer.get(), 0);
	}
}
