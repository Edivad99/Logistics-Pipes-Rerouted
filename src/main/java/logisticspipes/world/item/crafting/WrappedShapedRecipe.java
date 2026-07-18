package logisticspipes.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public abstract class WrappedShapedRecipe extends ShapedRecipe {
  private final ShapedRecipe internal;

  protected WrappedShapedRecipe(ShapedRecipe internal) {
    super(internal.getGroup(), internal.category(), internal.pattern, internal.getResultItem(null),
        internal.showNotification());
    this.internal = internal;
  }

  public ShapedRecipe getInternal() {
    return this.internal;
  }

  public abstract ItemStack assemble(CraftingInput input, HolderLookup.Provider provider);

  public boolean matches(CraftingInput input, Level world) {
    return this.internal.matches(input, world) && !this.assemble(input, world.registryAccess()).isEmpty();
  }

  public boolean canCraftInDimensions(int width, int height) {
    return this.internal.canCraftInDimensions(width, height);
  }

  public ItemStack getResultItem(HolderLookup.Provider provider) {
    return this.internal.getResultItem(provider);
  }

  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    return this.internal.getRemainingItems(input);
  }

  public NonNullList<Ingredient> getIngredients() {
    return this.internal.getIngredients();
  }

  public boolean isSpecial() {
    return this.internal.isSpecial();
  }

  public ItemStack getToastSymbol() {
    return this.internal.getToastSymbol();
  }

  public int getWidth() {
    return this.internal.getWidth();
  }

  public int getHeight() {
    return this.internal.getHeight();
  }

  public boolean isIncomplete() {
    return this.internal.isIncomplete();
  }
}
