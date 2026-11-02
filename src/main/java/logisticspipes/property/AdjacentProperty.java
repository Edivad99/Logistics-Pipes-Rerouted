package logisticspipes.property;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.property.ValueProperty;
import logisticspipes.connection.DynamicAdjacent;
import logisticspipes.connection.NoAdjacent;
import logisticspipes.connection.SingleAdjacent;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class AdjacentProperty extends ValueProperty<Adjacent> {

    private final CoreRoutedPipe pipe;
    private final String tagKey;

    public AdjacentProperty(CoreRoutedPipe pipe, String tagKey) {
        this(NoAdjacent.INSTANCE, pipe, tagKey);
    }

    public AdjacentProperty(Adjacent defaultValue, CoreRoutedPipe pipe, String tagKey) {
        super(defaultValue);
        this.pipe = pipe;
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public Adjacent copyValue() {
        return getValue(); // Adjacent is immutable
    }

    @Override
    public AdjacentProperty copyProperty() {
        return new AdjacentProperty(getValue(), pipe, tagKey);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.list(tagKey, Codec.STRING).ifPresent(stored -> {
            // serialize writes one entry per Direction, in Direction.values() order
            List<String> names = stored.stream().toList();
            Map<Direction, ConnectionType> connected = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                if (dir.ordinal() < names.size() && !names.get(dir.ordinal()).isBlank()) {
                    connected.put(dir, ConnectionType.valueOf(names.get(dir.ordinal())));
                }
            }
            setValue(toAdjacent(connected));
        });
    }

    private Adjacent toAdjacent(Map<Direction, ConnectionType> connected) {
        if (connected.isEmpty()) {
            return NoAdjacent.INSTANCE;
        }
        if (connected.size() == 1) {
            Map.Entry<Direction, ConnectionType> only = connected.entrySet().iterator().next();
            return new SingleAdjacent(pipe, only.getKey(), only.getValue());
        }
        @Nullable ConnectionType[] cache = new ConnectionType[Direction.values().length];
        connected.forEach((dir, type) -> cache[dir.get3DDataValue()] = type);
        return new DynamicAdjacent(pipe, cache);
    }

    @Override
    public void serialize(ValueOutput output) {
        ValueOutput.TypedOutputList<String> list = output.list(tagKey, Codec.STRING);
        if (getValue() == NoAdjacent.INSTANCE) {
            return;
        }
        for (Direction dir : Direction.values()) {
            ConnectionType connectionType = getValue().get(dir);
            list.add(connectionType == null ? "" : connectionType.name());
        }
    }

    public @Nullable Direction getDirectionOrNull() {
        return getValue() instanceof SingleAdjacent singleAdjacent ? singleAdjacent.getDir() : null;
    }
}
