package logisticspipes.api.property;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EnumProperty<E extends Enum<E>> extends ValueProperty<E> {

    private final E defaultValue;
    private final String tagKey;
    private final E[] enumValues;

    public EnumProperty(E defaultValue, String tagKey, E[] enumValues) {
        super(defaultValue);
        this.defaultValue = defaultValue;
        this.tagKey = tagKey;
        this.enumValues = enumValues;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    private E atOrDefault(int ordinal) {
        return ordinal >= 0 && ordinal < enumValues.length ? enumValues[ordinal] : defaultValue;
    }

    @Override
    public void deserialize(ValueInput input) {
        setValue(atOrDefault(input.getIntOr(tagKey, getValue().ordinal())));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt(tagKey, getValue().ordinal());
    }

    @Override
    public E copyValue() {
        return getValue();
    }

    @Override
    public EnumProperty<E> copyProperty() {
        EnumProperty<E> copy = new EnumProperty<>(defaultValue, tagKey, enumValues);
        copy.setValue(copyValue());
        return copy;
    }

    public E next() {
        int nextOrdinal = getValue().ordinal() + 1;
        E next = nextOrdinal < enumValues.length ? enumValues[nextOrdinal] : enumValues[0];
        setValue(next);
        return next;
    }
}
