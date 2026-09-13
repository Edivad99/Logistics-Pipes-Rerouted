package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

public interface IOrderManagerContentReceiver {

	void setOrderManagerContent(Collection<@Nullable ItemIdentifierStack> allItems);
}
