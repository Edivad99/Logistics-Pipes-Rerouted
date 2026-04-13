package logisticspipes.interfaces;

import net.minecraft.nbt.CompoundTag;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.property.StringListProperty;

public interface IStringBasedModule {

	StringListProperty stringListProperty();

	String getStringForItem(ItemIdentifier ident);

	void listChanged();

	void readFromNBT(CompoundTag nbt);
}
