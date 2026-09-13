package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A screen showing the items an orderer can currently request.
 */
public interface IAvailableItemsReceiver {

    void setAvailableItems(Collection<@Nullable ItemIdentifierStack> allItems);
}
