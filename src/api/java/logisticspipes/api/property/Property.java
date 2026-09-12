package logisticspipes.api.property;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArraySet;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Properties follow a standard observer pattern by notifying on every write access.
 */
public interface Property<V extends @Nullable Object> extends ValueIOSerializable {

    String getTagKey();

    CopyOnWriteArraySet<ObserverCallback<V>> getPropertyObservers();

    default void iChanged() {
        getPropertyObservers().forEach(observer -> observer.accept(this));
    }

    default boolean addObserver(ObserverCallback<V> callback) {
        return getPropertyObservers().add(callback);
    }

    V copyValue();

    /**
     * Copies the property and its current value. Must always return property of same class as the callee.
     */
    Property<? extends V> copyProperty();
}
