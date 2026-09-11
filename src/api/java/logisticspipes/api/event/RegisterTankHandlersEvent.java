package logisticspipes.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import logisticspipes.api.provider.ISpecialTankHandler;
import logisticspipes.api.provider.ISpecialTankUtilProvider;

/**
 * Fired on the mod event bus so addons can teach Logistics Pipes about tanks it cannot reach through
 * the fluid handler capability -- storage networks above all.
 *
 * <p>It is fired once during startup, after every mod has been constructed, and both sets are closed
 * when it returns.
 *
 * <pre>{@code
 * @SubscribeEvent
 * public static void onRegisterTankHandlers(RegisterTankHandlersEvent event) {
 *     event.register(new MyNetworkTankUtilProvider());
 * }
 * }</pre>
 */
public class RegisterTankHandlersEvent extends Event implements IModBusEvent {

    private final List<ISpecialTankUtilProvider> tankUtilProviders = new ArrayList<>();
    private final List<ISpecialTankHandler> handlers = new ArrayList<>();

    /** Adds a provider. Providers are asked in registration order, and the first match answers. */
    public void register(ISpecialTankUtilProvider provider) {
        tankUtilProviders.add(Objects.requireNonNull(provider, "provider"));
    }

    /** Adds a handler. Handlers are asked in registration order, and the first match answers. */
    public void register(ISpecialTankHandler handler) {
        handlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Everything registered, in order.
     *
     * <p>For Logistics Pipes itself to collect once the event is over; an addon has no reason to
     * call it.
     */
    public List<ISpecialTankUtilProvider> registeredProviders() {
        return List.copyOf(tankUtilProviders);
    }

    /** @see #registeredProviders() */
    public List<ISpecialTankHandler> registeredHandlers() {
        return List.copyOf(handlers);
    }
}
