package logisticspipes.api.property;

import java.util.UUID;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class UUIDProperty extends ValueProperty<UUID> {

    public static final UUID ZERO = new UUID(0L, 0L);

    private final String tagKey;

    public UUIDProperty(@Nullable UUID initialValue, String tagKey) {
        super(initialValue == null ? ZERO : initialValue);
        this.tagKey = tagKey;
    }

    public static boolean isZero(UUID uuid) {
        return ZERO.equals(uuid);
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public void deserialize(ValueInput input) {
        String stored = input.getStringOr(tagKey, "");
        if (!stored.isEmpty()) {
            setValue(UUID.fromString(stored));
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString(tagKey, getValue().toString());
    }

    @Override
    public UUID copyValue() {
        return getValue();
    }

    @Override
    public UUIDProperty copyProperty() {
        return new UUIDProperty(copyValue(), tagKey);
    }

    public boolean isZero() {
        return isZero(getValue());
    }

    public void zero() {
        setValue(ZERO);
    }
}
