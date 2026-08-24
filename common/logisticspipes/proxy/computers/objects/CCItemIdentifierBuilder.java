package logisticspipes.proxy.computers.objects;

import java.util.List;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.item.ItemIdentifier;

@CCType(name = "ItemIdentifierBuilder")
public class CCItemIdentifierBuilder implements ILPCCTypeHolder {

	private final Object[] ccTypeHolder = new Object[1];
	private int itemID = 0;
	private String itemIDName = null;
	private int itemData = 0;

	@CCCommand(description = "Set the itemID for this ItemIdentifierBuilder")
	public void setItemID(Double id) {
		itemID = id.intValue();
		itemIDName = null;
	}

	@CCCommand(description = "Set the itemID for this ItemIdentifierBuilder")
	public void setItemID(String id) {
		itemID = 0;
		itemIDName = id;
	}

	@CCCommand(description = "Returns the itemID (String or Int) for this ItemIdentifierBuilder")
	public Object getItemID() {
		if (itemIDName != null) {
			return itemIDName;
		}
		return itemID;
	}

	@CCCommand(description = "Set the item data/damage for this ItemIdentifierBuilder")
	public void setItemData(Double data) {
		itemData = data.intValue();
	}

	@CCCommand(description = "Returns the item data/damage for this ItemIdentifierBuilder")
	public int getItemData() {
		return itemData;
	}

	@CCCommand(description = "Returns the ItemIdentifier for this ItemIdentifierBuilder")
	public ItemIdentifier build() {
		Item item;
		if (itemIDName != null) {
			item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemIDName));
		} else {
			item = BuiltInRegistries.ITEM.byId(itemID);
		}
		if (item == null) {
			throw new UnsupportedOperationException("Not a valid ItemIdentifier");
		}
		// The scripting API still speaks damage, which in 1.21 is just the DAMAGE component.
		if (itemData == 0) {
			return ItemIdentifier.get(item);
		}
		return ItemIdentifier.get(item, DataComponentPatch.builder().set(DataComponents.DAMAGE, itemData).build());
	}

	@CCCommand(description = "Returns a list of all ItemIdentifier with an NBT tag matching the given Item ID and data")
	public List<ItemIdentifier> matchingNBTIdentifier() {
		Item item = BuiltInRegistries.ITEM.byId(itemID);
		if (item == null) {
			throw new UnsupportedOperationException("Not a valid ItemIdentifier");
		}
		return ItemIdentifier.getMatchingNBTIdentifier(item, itemData);
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

}
