package logisticspipes.world.item.crafting;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

public abstract class WrappedShapedRecipe extends ShapedRecipe {

    private final ShapedRecipe internal;

    protected WrappedShapedRecipe(ShapedRecipe internal) {
        super(new Recipe.CommonInfo(internal.showNotification()),
            new CraftingRecipe.CraftingBookInfo(internal.category(), internal.group()),
            internal.pattern, internal.result);
        this.internal = internal;
    }

    public ShapedRecipe getInternal() {
        return this.internal;
    }

    @Override
    public abstract ItemStack assemble(CraftingInput input);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.internal.matches(input, level) && !this.assemble(input).isEmpty();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return this.internal.getRemainingItems(input);
    }

    @Override
    public List<Optional<Ingredient>> getIngredients() {
        return this.internal.getIngredients();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return this.internal.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return this.internal.display();
    }

    @Override
    public boolean isSpecial() {
        return this.internal.isSpecial();
    }

    @Override
    public int getWidth() {
        return this.internal.getWidth();
    }

    @Override
    public int getHeight() {
        return this.internal.getHeight();
    }
}
