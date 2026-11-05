package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A client-side module told what its inventory holds.
 */
public interface IModuleInventoryReceive {

    void handleInvContent(Collection<@Nullable ItemIdentifierStack> allItems);
}
