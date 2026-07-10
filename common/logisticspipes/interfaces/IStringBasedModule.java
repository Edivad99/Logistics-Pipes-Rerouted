package logisticspipes.interfaces;

import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.nbt.CompoundTag;
import network.rs485.logisticspipes.property.StringListProperty;

public interface IStringBasedModule {

	StringListProperty stringListProperty();

	String getStringForItem(ItemIdentifier ident);

	void listChanged();

	void readFromNBT(CompoundTag nbt);
}
