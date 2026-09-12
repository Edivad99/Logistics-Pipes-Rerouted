package logisticspipes.api.property.layer;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.ValueProperty;

public interface ValuePropertyOverlay<T extends @Nullable Object, P extends ValueProperty<T>> extends PropertyOverlay<T, P> {

    T get();

    void set(T value);
}
