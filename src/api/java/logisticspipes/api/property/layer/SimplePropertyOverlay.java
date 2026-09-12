package logisticspipes.api.property.layer;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.Property;

public class SimplePropertyOverlay<T extends @Nullable Object, P extends Property<T>> implements PropertyOverlay<T, P> {

    private final P sourceProperty;

    private @Nullable P copiedProperty;

    public SimplePropertyOverlay(P sourceProperty) {
        this.sourceProperty = sourceProperty;
    }

    @Override
    public <V> V read(Function<P, V> func) {
        P copied = copiedProperty;
        return func.apply(copied != null ? copied : sourceProperty);
    }

    @SuppressWarnings("unchecked") // as specified by copyProperty
    @Override
    public <V> V write(Function<P, V> func) {
        P copied = copiedProperty;
        if (copied == null) {
            copied = (P) sourceProperty.copyProperty();
            copiedProperty = copied;
        }
        return func.apply(copied);
    }

    @Override
    public boolean isWriteMode() {
        return copiedProperty != null;
    }
}
