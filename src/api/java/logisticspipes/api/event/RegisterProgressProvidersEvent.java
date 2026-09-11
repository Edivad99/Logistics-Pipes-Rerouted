package logisticspipes.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import logisticspipes.api.provider.IGenericProgressProvider;

/**
 * Fired on the mod event bus so addons can teach Logistics Pipes to read the progress of machines
 * it does not know about.
 *
 * <p>It is fired once during startup, after every mod has been constructed, and the set of readers
 * is closed when it returns. Listening for it is the only supported way in: reaching into the
 * internals to register later is a race against the pipes that read them.
 *
 * <pre>{@code
 * @SubscribeEvent
 * public static void onRegisterProgressProviders(RegisterProgressProvidersEvent event) {
 *     event.register(new MyMachineProgressProvider());
 * }
 * }</pre>
 */
public class RegisterProgressProvidersEvent extends Event implements IModBusEvent {

    private final List<IGenericProgressProvider> providers = new ArrayList<>();

    /**
     * Adds a reader. Readers are asked in registration order, and the first match answers.
     */
    public void register(IGenericProgressProvider provider) {
        providers.add(Objects.requireNonNull(provider, "provider"));
    }

    /**
     * Everything registered, in order.
     *
     * <p>For Logistics Pipes itself to collect once the event is over; an addon has no reason to
     * call it.
     */
    public List<IGenericProgressProvider> registered() {
        return List.copyOf(providers);
    }
}
