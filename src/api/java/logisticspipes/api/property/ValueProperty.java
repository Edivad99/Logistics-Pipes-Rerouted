package logisticspipes.api.property;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import lombok.Getter;

public abstract class ValueProperty<V extends @Nullable Object> implements Property<V> {

    private final CopyOnWriteArraySet<ObserverCallback<V>> propertyObservers = new CopyOnWriteArraySet<>();

    @Getter
    private V value;

    protected ValueProperty(V initialValue) {
        this.value = initialValue;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<V>> getPropertyObservers() {
        return propertyObservers;
    }

    public void setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        if (!Objects.equals(oldValue, newValue)) {
            iChanged();
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValueProperty<?> that
            && getTagKey().equals(that.getTagKey())
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, getTagKey());
    }

    @Override
    public String toString() {
        return "Property(" + value + ")";
    }
}
