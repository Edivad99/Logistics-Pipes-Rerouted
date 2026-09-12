package logisticspipes.api.property.layer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.api.property.PropertyHolder;
import logisticspipes.api.property.ValueProperty;

/**
 * The property is created with a list of properties that are not being written to.
 * A layer of a property can be retrieved by calling one of the {@code overlay} methods with the
 * original property. Whenever a layer receives a write access, the underlying property will be
 * copied to a new property on the upper layer. A list of changed properties can be retrieved with
 * {@link #getProperties()}, which e.g. can be used with
 * {@code logisticspipes.network.to_server.pipe.SetPipePropertiesMessage}.
 */
public class PropertyLayer implements PropertyHolder {

    private final List<Property<?>> lowerLayer;
    private final List<@Nullable Property<?>> upperLayer;
    private final BitSet changedIndices;
    private final Map<Integer, ObserverCallback<?>> observersToRemove = new HashMap<>();

    public PropertyLayer(Collection<? extends Property<?>> propertiesIn) {
        this.lowerLayer = List.copyOf(propertiesIn);
        this.upperLayer = new ArrayList<>(Collections.nCopies(lowerLayer.size(), null));
        this.changedIndices = new BitSet(lowerLayer.size());
    }

    /**
     * A list consisting only of changed properties on this layer.
     */
    @Override
    public List<Property<?>> getProperties() {
        return changedIndices.stream()
            .<Property<?>>mapToObj(idx -> Objects.requireNonNull(upperLayer.get(idx)))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private void prepareWrite(int idx) {
        Property<Object> upperLayerProperty = (Property<Object>) lowerLayer.get(idx).copyProperty();
        upperLayer.set(idx, upperLayerProperty);
        upperLayerProperty.addObserver(ignored -> {
            // set to changed once the copied property is actually changed
            changedIndices.set(idx);
            ObserverCallback<Object> moved = (ObserverCallback<Object>) observersToRemove.remove(idx);
            if (moved != null) {
                lowerLayer.get(idx).getPropertyObservers().remove(moved);
                upperLayerProperty.addObserver(moved);
                moved.accept(upperLayerProperty);
            }
        });
    }

    private int lookupIndex(Property<?> prop, List<Property<?>> propList) {
        for (int idx = 0; idx < propList.size(); idx++) {
            if (propList.get(idx) == prop) {
                return idx;
            }
        }
        throw new IllegalArgumentException("Property <" + prop + "> not in this layer");
    }

    public <T extends @Nullable Object, P extends ValueProperty<T>> ValuePropertyOverlay<T, P> overlay(P valueProp) {
        return new ValuePropertyOverlayImpl<>(lookupIndex(valueProp, lowerLayer));
    }

    public <T extends @Nullable Object, P extends Property<T>> PropertyOverlay<T, P> overlayOf(P prop) {
        return new PropertyOverlayImpl<>(lookupIndex(prop, lowerLayer));
    }

    @SuppressWarnings("unchecked")
    public <T extends @Nullable Object, P extends Property<T>> P writeProp(P prop) {
        int idx = lookupIndex(prop, lowerLayer);
        if (!changedIndices.get(idx)) {
            prepareWrite(idx);
        }
        return (P) Objects.requireNonNull(upperLayer.get(idx));
    }

    @SuppressWarnings("unchecked")
    public <T extends @Nullable Object, P extends Property<T>> void addObserver(P prop, ObserverCallback<T> observer) {
        int idx = lookupIndex(prop, lowerLayer);
        if (changedIndices.get(idx)) {
            ((P) Objects.requireNonNull(upperLayer.get(idx))).addObserver(observer);
        } else {
            observersToRemove.put(idx, observer);
            ((P) lowerLayer.get(idx)).addObserver(observer);
        }
    }

    public void unregister() {
        observersToRemove.forEach((idx, observer) ->
            lowerLayer.get(idx).getPropertyObservers().remove(observer));
    }

    public class PropertyOverlayImpl<T extends @Nullable Object, P extends Property<T>> implements PropertyOverlay<T, P> {

        private final int idx;

        PropertyOverlayImpl(int idx) {
            this.idx = idx;
        }

        @SuppressWarnings("unchecked")
        protected P lookupRead() {
            return changedIndices.get(idx)
                ? (P) Objects.requireNonNull(upperLayer.get(idx))
                : (P) lowerLayer.get(idx);
        }

        @SuppressWarnings("unchecked")
        protected P lookupWrite() {
            if (!changedIndices.get(idx)) {
                prepareWrite(idx);
            }
            return (P) Objects.requireNonNull(upperLayer.get(idx));
        }

        @Override
        public <V> V read(Function<P, V> func) {
            return func.apply(lookupRead());
        }

        @Override
        public <V> V write(Function<P, V> func) {
            return func.apply(lookupWrite());
        }

        @Override
        public boolean isWriteMode() {
            return changedIndices.get(idx);
        }
    }

    public class ValuePropertyOverlayImpl<T extends @Nullable Object, P extends ValueProperty<T>> extends PropertyOverlayImpl<T, P>
        implements ValuePropertyOverlay<T, P> {

        ValuePropertyOverlayImpl(int idx) {
            super(idx);
        }

        @Override
        public T get() {
            return lookupRead().getValue();
        }

        @Override
        public void set(T value) {
            lookupWrite().setValue(value);
        }
    }
}
