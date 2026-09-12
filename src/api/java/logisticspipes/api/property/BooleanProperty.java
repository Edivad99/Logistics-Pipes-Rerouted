package logisticspipes.api.property;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BooleanProperty extends ValueProperty<Boolean> {

    private final String tagKey;

    public BooleanProperty(boolean initialValue, String tagKey) {
        super(initialValue);
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public void deserialize(ValueInput input) {
        setValue(input.getBooleanOr(tagKey, getValue()));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean(tagKey, getValue());
    }

    @Override
    public Boolean copyValue() {
        return getValue();
    }

    @Override
    public BooleanProperty copyProperty() {
        return new BooleanProperty(copyValue(), tagKey);
    }

    public boolean toggle() {
        boolean toggled = !getValue();
        setValue(toggled);
        return toggled;
    }
}
