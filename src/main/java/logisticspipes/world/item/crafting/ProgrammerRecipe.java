package logisticspipes.world.item.crafting;

import java.util.List;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;

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
                return program != null && program.equals(getRequiredProgram());
            }
        }
        return false;
    }

    private String getRequiredProgram() {
        return BuiltInRegistries.ITEM.getKey(this.result.item().value()).toString();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.result.create();
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) LPRecipeSerializers.PROGRAMMER_RECIPE.get();
    }

    @Override
    public List<RecipeDisplay> display() {
        ItemStackTemplate programmer = new ItemStackTemplate(LPItems.LOGISTICS_PROGRAMMER.get(), 1,
            DataComponentPatch.builder().set(LPDataComponents.RECIPE_TARGET.get(), getRequiredProgram()).build());
        SlotDisplay programmerSlot = new SlotDisplay.ItemStackSlotDisplay(programmer);
        List<SlotDisplay> slots = getIngredients().stream()
            .map(ingredient -> ingredient
                .map(it -> it.test(LPItems.LOGISTICS_PROGRAMMER.toStack()) ? programmerSlot : it.display())
                .orElse(SlotDisplay.Empty.INSTANCE))
            .toList();
        return List.of(new ShapedCraftingRecipeDisplay(
            getWidth(),
            getHeight(),
            slots,
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    public static RecipeSerializer<ProgrammerRecipe> createSerializer() {
        return new RecipeSerializer<>(
            ShapedRecipe.SERIALIZER.codec().xmap(ProgrammerRecipe::new, ProgrammerRecipe::getInternal),
            ShapedRecipe.SERIALIZER.streamCodec().map(ProgrammerRecipe::new, ProgrammerRecipe::getInternal));
    }
}
