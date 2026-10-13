package logisticspipes.data.recipes.builders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import org.jspecify.annotations.Nullable;

import logisticspipes.world.item.crafting.ProgrammerRecipe;

/**
 * Datagen builder for {@link ProgrammerRecipe}, a shaped recipe that additionally requires the
 * programmer in the grid to be programmed for the result item.
 *
 * <p>Up to 1.21.1 this only had to subclass {@link net.minecraft.data.recipes.ShapedRecipeBuilder}
 * and override {@code save}. 1.21.3 made that class's constructors private,
 * so the shaped-pattern bookkeeping is duplicated here instead.
 */
public class ProgrammerRecipeBuilder implements RecipeBuilder {

    private final HolderGetter<Item> items;
    private final RecipeCategory category;
    private final ItemStackTemplate resultStack;
    private final List<String> rows = new ArrayList<>();
    private final Map<Character, Ingredient> key = new LinkedHashMap<>();
    private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
    @Nullable
    private String group;
    private boolean showNotification = true;

    private ProgrammerRecipeBuilder(HolderGetter<Item> items, RecipeCategory category, ItemStackTemplate result) {
        this.items = items;
        this.category = category;
        this.resultStack = result;
    }

    public static ProgrammerRecipeBuilder shaped(HolderGetter<Item> items, RecipeCategory category, ItemLike result) {
        return shaped(items, category, result, 1);
    }

    public static ProgrammerRecipeBuilder shaped(HolderGetter<Item> items, RecipeCategory category, ItemLike result,
        int count) {
        return new ProgrammerRecipeBuilder(items, category, new ItemStackTemplate(result.asItem(), count));
    }

    public static ProgrammerRecipeBuilder shaped(HolderGetter<Item> items, RecipeCategory category,
        ItemStackTemplate result) {
        return new ProgrammerRecipeBuilder(items, category, result);
    }

    public ProgrammerRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        return this.define(symbol, Ingredient.of(this.items.getOrThrow(tag)));
    }

    public ProgrammerRecipeBuilder define(Character symbol, ItemLike item) {
        return this.define(symbol, Ingredient.of(item));
    }

    public ProgrammerRecipeBuilder define(Character symbol, Ingredient ingredient) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        } else if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        }
        this.key.put(symbol, ingredient);
        return this;
    }

    public ProgrammerRecipeBuilder pattern(String pattern) {
        if (!this.rows.isEmpty() && pattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        }
        this.rows.add(pattern);
        return this;
    }

    @Override
    public ProgrammerRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.advancementBuilder.unlockedBy(name, criterion);
        return this;
    }

    @Override
    public ProgrammerRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    public ProgrammerRecipeBuilder showNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return RecipeBuilder.getDefaultRecipeId(this.resultStack);
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> id) {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(this.key, this.rows);
        ShapedRecipe shapedrecipe = new ShapedRecipe(
            RecipeBuilder.createCraftingCommonInfo(this.showNotification),
            RecipeBuilder.createCraftingBookInfo(this.category, this.group),
            pattern,
            this.resultStack);
        recipeOutput.accept(id, new ProgrammerRecipe(shapedrecipe),
            this.advancementBuilder.build(recipeOutput, id, this.category));
    }
}
