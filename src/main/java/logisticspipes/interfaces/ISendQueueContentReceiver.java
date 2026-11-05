package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A client-side pipe told what is waiting in its send queue, for its HUD.
 */
public interface ISendQueueContentReceiver {

    void handleSendQueueItemIdentifierList(Collection<@Nullable ItemIdentifierStack> allItems);
}
