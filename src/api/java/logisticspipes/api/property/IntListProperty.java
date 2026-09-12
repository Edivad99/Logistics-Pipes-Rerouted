package logisticspipes.api.property;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class IntListProperty extends ListProperty<Integer> {

    private final String tagKey;

    public IntListProperty(String tagKey) {
        this(tagKey, new ArrayList<>());
    }

    private IntListProperty(String tagKey, List<Integer> list) {
        super(list);
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    public boolean replaceContent(int[] arr) {
        List<Integer> boxed = new ArrayList<>(arr.length);
        for (int value : arr) {
            boxed.add(value);
        }
        return replaceContent(boxed);
    }

    public int[] getArray() {
        int[] arr = new int[list.size()];
        for (int idx = 0; idx < arr.length; idx++) {
            arr[idx] = list.get(idx);
        }
        return arr;
    }

    public Integer increase(int index, int by) {
        return set(index, get(index) + by);
    }

    @Override
    public Integer defaultValue(int idx) {
        return 0;
    }

    @Override
    public void deserialize(ValueInput input) {
        input.getIntArray(tagKey).ifPresent(this::replaceContent);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putIntArray(tagKey, getArray());
    }

    @Override
    public Integer copyValue(Integer obj) {
        return obj;
    }

    @Override
    public IntListProperty copyProperty() {
        return new IntListProperty(tagKey, copyValue());
    }

    @Override
    public Integer readSingleFromNBT(ValueInput input, String key) {
        return input.getIntOr(key, 0);
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, Integer value) {
        output.putInt(key, value);
    }
}
