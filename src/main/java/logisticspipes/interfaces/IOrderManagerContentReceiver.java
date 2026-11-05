package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A client-side pipe told which orders it still has to fill, for its HUD.
 */
public interface IOrderManagerContentReceiver {

    void setOrderManagerContent(Collection<@Nullable ItemIdentifierStack> allItems);
}
