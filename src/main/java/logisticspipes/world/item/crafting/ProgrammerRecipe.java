package logisticspipes.world.item.crafting;

import com.mojang.serialization.MapCodec;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class ProgrammerRecipe extends WrappedShapedRecipe {

  public ProgrammerRecipe(ShapedRecipe internal) {
    super(internal);
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    if (!super.matches(input, level)) {
      return false;
    }

    for (int i = 0; i < input.size(); i++) {
      ItemStack stack = input.getItem(i);
      if (stack.is(LPItems.LOGISTICS_PROGRAMMER)) {
        String program = stack.get(LPDataComponents.RECIPE_TARGET);
        return program != null &&
            program.equals(getRequiredProgram());
      }
    }
    return false;
  }

//  @Override
//  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
//    NonNullList<ItemStack> resultItems = super.getRemainingItems(input);
//    for (int i = 0; i < input.size(); i++) {
//      ItemStack stack = input.getItem(i);
//      if (stack.is(LPItems.LOGISTICS_PROGRAMMER)) {
//        resultItems.set(i, stack);
//      }
//    }
//    return resultItems;
//  }

  private String getRequiredProgram() {
    return BuiltInRegistries.ITEM.getKey(getResultItem(null).getItem()).toString();
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
    return getResultItem(provider).copy();
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return LPRecipeSerializers.PROGRAMMER_RECIPE.get();
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    NonNullList<Ingredient> ingredients = NonNullList.create();
    for (Ingredient ingredient : super.getIngredients()) {
      if (ingredient.test(LPItems.LOGISTICS_PROGRAMMER.toStack())) {
        ItemStack programmer = LPItems.LOGISTICS_PROGRAMMER.toStack();
        programmer.set(LPDataComponents.RECIPE_TARGET.get(), getRequiredProgram());
        ingredients.add(Ingredient.of(programmer));
      } else {
        ingredients.add(ingredient);
      }
    }
    return ingredients;
  }

  public static class Serializer implements RecipeSerializer<ProgrammerRecipe> {
    private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

    @Override
    public MapCodec<ProgrammerRecipe> codec() {
      return vanilla.codec().xmap(ProgrammerRecipe::new, ProgrammerRecipe::getInternal);
    }


    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ProgrammerRecipe> streamCodec() {
      return vanilla.streamCodec().map(ProgrammerRecipe::new, ProgrammerRecipe::getInternal);
    }
  }
}
