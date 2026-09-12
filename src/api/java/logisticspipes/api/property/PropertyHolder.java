package logisticspipes.api.property;

import java.util.List;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Holds a {@link #getProperties()} list and serialises them through {@link ValueIOSerializable}.
 */
public interface PropertyHolder {

    List<Property<?>> getProperties();

    static void deserialize(ValueInput input, PropertyHolder holder) {
        PropertyUtil.deserialize(holder.getProperties(), input);
    }

    static void serialize(ValueOutput output, PropertyHolder holder) {
        PropertyUtil.serialize(holder.getProperties(), output);
    }
}
