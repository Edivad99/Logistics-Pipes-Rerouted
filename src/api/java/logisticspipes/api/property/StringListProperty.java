package logisticspipes.api.property;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class StringListProperty extends ListProperty<String> {

    private final String tagKey;

    public StringListProperty(String tagKey) {
        this(tagKey, new ArrayList<>());
    }

    private StringListProperty(String tagKey, List<String> list) {
        super(list);
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public String defaultValue(int idx) {
        return "";
    }

    @Override
    public String readSingleFromNBT(ValueInput input, String key) {
        return input.getStringOr(key, "");
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, String value) {
        output.putString(key, value);
    }

    @Override
    public String copyValue(String obj) {
        return obj;
    }

    @Override
    public StringListProperty copyProperty() {
        return new StringListProperty(tagKey, copyValue());
    }
}
