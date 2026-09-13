package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

public interface IModuleInventoryReceive {

	void handleInvContent(Collection<@Nullable ItemIdentifierStack> allItems);
}
