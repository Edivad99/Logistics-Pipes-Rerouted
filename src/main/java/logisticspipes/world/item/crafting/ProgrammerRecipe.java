package logisticspipes.world.item.crafting;

import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;

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
        return BuiltInRegistries.ITEM.getKey(this.result.getItem()).toString();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        return this.result.copy();
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return LPRecipeSerializers.PROGRAMMER_RECIPE.get();
    }

    @Override
    public List<RecipeDisplay> display() {
        ItemStack programmer = LPItems.LOGISTICS_PROGRAMMER.toStack();
        programmer.set(LPDataComponents.RECIPE_TARGET.get(), getRequiredProgram());
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
