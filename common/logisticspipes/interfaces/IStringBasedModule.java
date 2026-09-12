package logisticspipes.interfaces;

import net.minecraft.world.level.storage.ValueInput;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.api.property.StringListProperty;

public interface IStringBasedModule {

	StringListProperty stringListProperty();

	String getStringForItem(ItemIdentifier ident);

	void listChanged();

	void deserialize(ValueInput input);
}
