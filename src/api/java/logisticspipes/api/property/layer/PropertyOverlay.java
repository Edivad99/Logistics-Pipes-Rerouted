package logisticspipes.api.property.layer;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import logisticspipes.api.property.Property;

public interface PropertyOverlay<T extends @Nullable Object, P extends Property<T>> {

    <V> V read(Function<P, V> func);

    <V> V write(Function<P, V> func);

    boolean isWriteMode();

    /** {@link #read(Function)} for a func that returns nothing. */
    default void readVoid(Consumer<P> func) {
        read(prop -> {
            func.accept(prop);
            return Boolean.TRUE;
        });
    }

    /** {@link #write(Function)} for a func that returns nothing. */
    default void writeVoid(Consumer<P> func) {
        write(prop -> {
            func.accept(prop);
            return Boolean.TRUE;
        });
    }
}
