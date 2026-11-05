package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A client-side pipe told what the inventories next to it hold, for its HUD.
 */
public interface IChestContentReceiver {

    void setReceivedChestContent(Collection<@Nullable ItemIdentifierStack> allItems);
}
