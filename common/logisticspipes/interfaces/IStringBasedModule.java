package logisticspipes.interfaces;

import net.minecraft.world.level.storage.ValueInput;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.property.StringListProperty;

public interface IStringBasedModule {

	StringListProperty stringListProperty();

	String getStringForItem(ItemIdentifier ident);

	void listChanged();

	void deserialize(ValueInput input);
}
