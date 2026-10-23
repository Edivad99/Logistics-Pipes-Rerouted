package logisticspipes.crafting;

import java.util.List;

/**
 * The {@link ICraftingRecipeProvider}s collected from {@link RegisterCraftingRecipeProvidersEvent}.
 */
public final class CraftingRecipeProviders {

    private static List<ICraftingRecipeProvider> providers = List.of();

    private CraftingRecipeProviders() {
    }

    /** Collects everything registered on {@code event}, which is closed from here on. */
    public static void load(RegisterCraftingRecipeProvidersEvent event) {
        providers = event.registered();
    }

    /** The providers in registration order; empty until startup collected them. */
    public static List<ICraftingRecipeProvider> get() {
        return providers;
    }
}
