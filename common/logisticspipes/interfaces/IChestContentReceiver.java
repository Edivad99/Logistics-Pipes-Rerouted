package logisticspipes.interfaces;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

public interface IChestContentReceiver {

	void setReceivedChestContent(Collection<@Nullable ItemIdentifierStack> allItems);

}
