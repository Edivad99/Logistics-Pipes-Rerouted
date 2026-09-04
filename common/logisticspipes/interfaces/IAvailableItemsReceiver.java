package logisticspipes.interfaces;

import java.util.Collection;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A screen showing the items an orderer can currently request.
 */
public interface IAvailableItemsReceiver {

    void setAvailableItems(Collection<ItemIdentifierStack> allItems);
}
