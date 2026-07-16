package logisticspipes.data.recipes.builders;

import java.util.Objects;
import logisticspipes.world.item.crafting.ModuleRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

public class ModuleRecipeBuilder extends ShapedRecipeBuilder {

  public ModuleRecipeBuilder(RecipeCategory category, ItemLike result, int count) {
    this(category, new ItemStack(result, count));
  }

  public ModuleRecipeBuilder(RecipeCategory category, ItemStack result) {
    super(category, result);
  }

  public ModuleRecipeBuilder(RecipeCategory category, ItemStack result, int count) {
    super(category, result);
  }

  public static ModuleRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
    return shaped(category, result, 1);
  }

  public static ModuleRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
    return new ModuleRecipeBuilder(category, result, count);
  }

  public static ModuleRecipeBuilder shaped(RecipeCategory category, ItemStack result) {
    return new ModuleRecipeBuilder(category, result);
  }

  public ModuleRecipeBuilder define(Character symbol, TagKey<Item> tag) {
    return (ModuleRecipeBuilder) super.define(symbol, Ingredient.of(tag));
  }

  public ModuleRecipeBuilder define(Character symbol, ItemLike item) {
    return (ModuleRecipeBuilder) super.define(symbol, Ingredient.of(item));
  }

  @Override
  public void save(RecipeOutput recipeOutput, ResourceLocation id) {
    ShapedRecipePattern shapedrecipepattern = this.ensureValid(id);
    Advancement.Builder builder = recipeOutput.advancement()
        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
        .rewards(AdvancementRewards.Builder.recipe(id))
        .requirements(AdvancementRequirements.Strategy.OR);
    this.criteria.forEach(builder::addCriterion);
    ShapedRecipe shapedrecipe = new ShapedRecipe(
        Objects.requireNonNullElse(this.group, ""),
        RecipeBuilder.determineBookCategory(this.category),
        shapedrecipepattern,
        this.resultStack,
        this.showNotification);
    recipeOutput.accept(id, new ModuleRecipe(shapedrecipe),
        builder.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
  }
}
