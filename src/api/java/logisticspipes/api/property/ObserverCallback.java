package logisticspipes.api.property;

import org.jspecify.annotations.Nullable;

/** Notified on every write access to the {@link Property} it was registered on. */
@FunctionalInterface
public interface ObserverCallback<V extends @Nullable Object> {
    void accept(Property<V> property);
}
