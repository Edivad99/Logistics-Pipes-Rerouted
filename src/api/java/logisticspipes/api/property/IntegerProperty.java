package logisticspipes.api.property;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class IntegerProperty extends ValueProperty<Integer> {

    private final String tagKey;

    public IntegerProperty(int initialValue, String tagKey) {
        super(initialValue);
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public void deserialize(ValueInput input) {
        setValue(input.getIntOr(tagKey, getValue()));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt(tagKey, getValue());
    }

    @Override
    public Integer copyValue() {
        return getValue();
    }

    @Override
    public IntegerProperty copyProperty() {
        return new IntegerProperty(copyValue(), tagKey);
    }

    public int increase(int by) {
        int increased = getValue() + by;
        setValue(increased);
        return increased;
    }
}
