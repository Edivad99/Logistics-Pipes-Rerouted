package logisticspipes.interfaces;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;

/**
 * A 3x3 grid the player edits to pick a recipe.
 *
 * <p>Two unrelated things offer one: the crafting table block, and the request table pipe. They
 * share no supertype, so every message that edited a grid had to ask about both and reach into
 * whichever it found -- four packets each carrying their own copy of the same instanceof ladder.
 */
public interface ICraftingRecipeGrid {

	ItemIdentifierInventory getMatrix();

	/** Which of the recipes the grid matches is currently selected, or null when it matches none. */
	@Nullable ItemIdentifier getTargetType();

	void setTargetType(@Nullable ItemIdentifier targetType);

	/** Works out what the grid currently crafts. Call after changing the grid or the target. */
	void cacheRecipe();

	/** Steps to the next or previous recipe the grid could produce. */
	void cycleRecipe(boolean down);

	/** Fills the grid from a recipe the player picked in a recipe viewer. */
	void handleRecipeViewerImport(NonNullList<ItemStack> content);
}
