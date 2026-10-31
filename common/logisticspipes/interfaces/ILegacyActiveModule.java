package logisticspipes.interfaces;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.utils.item.ItemIdentifier;

public interface ILegacyActiveModule {

	void onBlockRemoval();

	void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filter);

	@Nullable LogisticsOrder fullFill(ItemPromise promise, IRequestItems destination, @Nullable IAdditionalTargetInformation info);

	void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter);
}
