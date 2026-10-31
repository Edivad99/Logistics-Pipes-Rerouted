package logisticspipes.interfaces;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.OrderTree;
import logisticspipes.request.resources.IResource;

public interface IRequestWatcher {

	void handleOrderList(@Nullable IResource stack, OrderTree orders);

	void handleClientSideListInfo(int id, IResource stack, OrderTree orders);

	void handleClientSideRemove(int id);
}
