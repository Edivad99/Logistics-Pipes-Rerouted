package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

public interface ISendQueueContentReceiver {

	void handleSendQueueItemIdentifierList(Collection<@Nullable ItemIdentifierStack> allItems);
}
