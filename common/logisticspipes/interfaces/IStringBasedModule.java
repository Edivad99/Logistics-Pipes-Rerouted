package logisticspipes.interfaces;

import net.minecraft.world.level.storage.ValueInput;

import logisticspipes.api.property.StringListProperty;
import logisticspipes.utils.item.ItemIdentifier;

public interface IStringBasedModule {

	StringListProperty stringListProperty();

	String getStringForItem(ItemIdentifier ident);

	void listChanged();

	void deserialize(ValueInput input);
}
