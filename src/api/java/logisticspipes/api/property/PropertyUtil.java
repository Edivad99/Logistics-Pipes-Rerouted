package logisticspipes.api.property;

import java.util.Collection;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Bulk operations over a collection of properties. */
public final class PropertyUtil {

    private PropertyUtil() {
    }

    @SuppressWarnings("unchecked") // the callback is invoked with the property it was registered on
    public static void addObserver(Collection<? extends Property<?>> properties, ObserverCallback<?> callback) {
        properties.forEach(prop -> ((Property<Object>) prop).addObserver((ObserverCallback<Object>) callback));
    }

    public static void removeObserver(Collection<? extends Property<?>> properties, ObserverCallback<?> callback) {
        properties.forEach(prop -> prop.getPropertyObservers().remove(callback));
    }

    public static void deserialize(Collection<? extends Property<?>> properties, ValueInput input) {
        properties.forEach(prop -> prop.deserialize(input));
    }

    public static void serialize(Collection<? extends Property<?>> properties, ValueOutput output) {
        properties.forEach(prop -> prop.serialize(output));
    }
}
