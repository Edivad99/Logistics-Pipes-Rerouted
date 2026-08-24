package logisticspipes.interfaces;

import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.ItemStack;

import logisticspipes.utils.item.ItemIdentifier;

public interface IInventoryUtil {

	int itemCount(ItemIdentifier item);

	Map<ItemIdentifier, Integer> getItemsAndCount();

	@Deprecated
    ItemStack getSingleItem(ItemIdentifier item);

	ItemStack getMultipleItems(ItemIdentifier item, int count);

	/**
	 * Checks to see if the item is inside the inventory. Used by the PolymorphicItemSink
	 * This includes slots that are limited to one item type but don't contain any items.
	 *
	 * @param item The item to check
	 * @return true if the item is inside the inventory
	 */
	boolean containsUndamagedItem(ItemIdentifier item);

	/**
	 * Inventory space count which terminates when space for max items are
	 * found.
	 *
	 * @return spaces found. If this is less than max, then there are only
	 * spaces for that amount.
	 * @param stack
	 */
	int roomForItem(ItemStack stack);

	Set<ItemIdentifier> getItems();

	//AbstractContainerMenu adapter
	int getContainerSize();

	ItemStack getItem(int slot);

	ItemStack removeItem(int slot, int amount);
}
