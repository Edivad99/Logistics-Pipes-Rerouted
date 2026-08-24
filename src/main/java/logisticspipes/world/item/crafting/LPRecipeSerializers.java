package logisticspipes.world.item.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;

public class LPRecipeSerializers {

    private static final DeferredRegister<RecipeSerializer<?>> deferredRegister =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, LPConstants.ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProgrammerRecipe>> PROGRAMMER_RECIPE =
        deferredRegister.register("programmer_recipe", ProgrammerRecipe::createSerializer);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }
}
