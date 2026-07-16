package logisticspipes.recipes;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public abstract class CraftingPartRecipes implements IRecipeProvider {

	private static List<CraftingParts> craftingPartList = null;

	public static List<CraftingParts> getCraftingPartList() {
		if (craftingPartList == null) {
			craftingPartList = new ArrayList<>();
			/*
			CraftingParts parts = SimpleServiceLocator.buildCraftProxy.getRecipeParts();
			// NO BC => NO RECIPES (for now)
			if (parts != null) {
				SimpleServiceLocator.IC2Proxy.addCraftingRecipes(parts);
				SimpleServiceLocator.thaumCraftProxy.addCraftingRecipes(parts);
				SimpleServiceLocator.ccProxy.addCraftingRecipes(parts);
				SimpleServiceLocator.buildCraftProxy.addCraftingRecipes(parts);

				RecipeManager.loadRecipes();
			}
			*/

			if (true) { // TODO: Add Config Option
				craftingPartList.add(new CraftingParts(
						new ItemStack(LPItems.CHIP_FPGA.get(), 1),
						new ItemStack(LPItems.CHIP_BASIC.get(), 1),
						new ItemStack(LPItems.CHIP_ADVANCED.get(), 1)));
			}
		}

		return craftingPartList;
	}

	@Override
	public final void loadRecipes() {
		getCraftingPartList().forEach(this::loadRecipes);
	}

	@Nonnull
	protected Ingredient programmerIngredient(String recipeTarget) {
		ItemStack programmerStack = new ItemStack(LPItems.LOGISTICS_PROGRAMMER.get());
		programmerStack.set(LPDataComponents.RECIPE_TARGET, recipeTarget);
		return Ingredient.of(programmerStack);
	}

	protected abstract void loadRecipes(CraftingParts parts);

}
