package logisticspipes.interfaces;

import net.minecraft.world.level.storage.ValueInput;

import logisticspipes.api.property.StringListProperty;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * A sink module that picks items by a name they belong under, such as their mod.
 */
public interface IStringBasedModule {

    StringListProperty stringListProperty();

    /**
     * @return the name {@code item} goes under, which the list is matched against
     */
    String getStringForItem(ItemIdentifier item);

    void listChanged();

    void deserialize(ValueInput input);
}
