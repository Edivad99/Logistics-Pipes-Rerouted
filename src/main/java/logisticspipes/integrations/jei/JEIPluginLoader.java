package logisticspipes.integrations.jei;

import net.minecraft.world.item.ItemStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import org.jspecify.annotations.Nullable;

public class JEIPluginLoader {

    @Nullable
    private static IJeiRuntime jeiRuntime = null;

    public static void setRuntime(IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static void clearRuntime() {
        jeiRuntime = null;
    }

    public static void showRecipe(ItemStack stack) {
        if (jeiRuntime == null || stack.isEmpty()) {
            return;
        }
        IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
        jeiRuntime.getRecipesGui().show(
            focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
    }
}
