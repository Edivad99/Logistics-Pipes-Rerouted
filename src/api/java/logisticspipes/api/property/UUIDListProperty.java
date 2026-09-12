package logisticspipes.api.property;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class UUIDListProperty extends ListProperty<UUID> {

    private final String tagKey;

    public UUIDListProperty(String tagKey) {
        this(tagKey, new ArrayList<>());
    }

    private UUIDListProperty(String tagKey, List<UUID> list) {
        super(list);
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public UUID defaultValue(int idx) {
        return UUIDProperty.ZERO;
    }

    @Override
    public UUID readSingleFromNBT(ValueInput input, String key) {
        return input.getString(key).map(UUID::fromString).orElse(UUIDProperty.ZERO);
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, UUID value) {
        output.putString(key, value.toString());
    }

    // UUID objects are immutable
    @Override
    public UUID copyValue(UUID obj) {
        return obj;
    }

    @Override
    public UUIDListProperty copyProperty() {
        return new UUIDListProperty(tagKey, copyValue());
    }

    public boolean isZero(int idx) {
        return UUIDProperty.isZero(get(idx));
    }

    public UUID zero(int idx) {
        return set(idx, UUIDProperty.ZERO);
    }
}
