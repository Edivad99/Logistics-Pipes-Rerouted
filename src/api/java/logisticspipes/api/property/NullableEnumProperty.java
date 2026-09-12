package logisticspipes.api.property;

import java.util.Objects;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class NullableEnumProperty<E extends Enum<E>> extends ValueProperty<@Nullable E> {

    private final @Nullable E defaultValue;
    private final String tagKey;
    private final E[] enumValues;

    public NullableEnumProperty(@Nullable E defaultValue, String tagKey, E[] enumValues) {
        super(defaultValue);
        this.defaultValue = defaultValue;
        this.tagKey = tagKey;
        this.enumValues = enumValues;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public void deserialize(ValueInput input) {
        input.getInt(tagKey).ifPresent(ordinal -> {
            if (ordinal == -1) {
                setValue(null);
            } else {
                setValue(ordinal >= 0 && ordinal < enumValues.length ? enumValues[ordinal] : defaultValue);
            }
        });
    }

    @Override
    public void serialize(ValueOutput output) {
        E current = getValue();
        output.putInt(tagKey, current == null ? -1 : current.ordinal());
    }

    @Override
    public @Nullable E copyValue() {
        return getValue();
    }

    @Override
    public NullableEnumProperty<E> copyProperty() {
        NullableEnumProperty<E> copy = new NullableEnumProperty<>(defaultValue, tagKey, enumValues);
        copy.setValue(copyValue());
        return copy;
    }

    /**
     * This will throw a NPE when the current value is null.
     */
    public E next() {
        int nextOrdinal = Objects.requireNonNull(getValue()).ordinal() + 1;
        E next = nextOrdinal < enumValues.length ? enumValues[nextOrdinal] : enumValues[0];
        setValue(next);
        return next;
    }
}
