package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Fired on the mod event bus so addons can let a crafting pipe import the recipe of a crafting block
 * next to it.
 *
 * <p>It is fired once during startup, and the set of providers is closed when it returns.
 *
 * <p>Not part of {@code src/api} yet: {@link ICraftingRecipeProvider} still speaks in terms of
 * internal inventory types.
 */
public class RegisterCraftingRecipeProvidersEvent extends Event implements IModBusEvent {

    private final List<ICraftingRecipeProvider> providers = new ArrayList<>();

    /** Adds a provider. Providers are asked in registration order, and the first match answers. */
    public void register(ICraftingRecipeProvider provider) {
        providers.add(Objects.requireNonNull(provider, "provider"));
    }

    List<ICraftingRecipeProvider> registered() {
        return List.copyOf(providers);
    }
}
