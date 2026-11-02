package logisticspipes.api.property;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class ValueProperty<V extends @Nullable Object> implements Property<V> {

    private final CopyOnWriteArraySet<ObserverCallback<V>> propertyObservers = new CopyOnWriteArraySet<>();

    private V value;

    protected ValueProperty(V initialValue) {
        this.value = initialValue;
    }

    // Written out rather than a Lombok @Getter: IntelliJ reads the generated getter as returning the
    // @Nullable bound of V, so every non-null subclass would warn on getValue().
    public V getValue() {
        return value;
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
