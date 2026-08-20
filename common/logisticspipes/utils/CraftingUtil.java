package logisticspipes.utils;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CraftingUtil {

    public static Collection<RecipeHolder<CraftingRecipe>> getRecipeList() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Collections.emptyList();
        }
        return server.getRecipeManager().recipeMap().byType(RecipeType.CRAFTING);
    }
}
