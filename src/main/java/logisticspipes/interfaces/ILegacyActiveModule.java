package logisticspipes.interfaces;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.order.Order;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * A module that provides items to the network, which a chassis answers requests through. The same
 * calls as {@link logisticspipes.interfaces.routing.IProvideItems}, for a module.
 */
public interface ILegacyActiveModule {

    void onBlockRemoval();

    void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters);

    /**
     * @return the order placed, or null when none could be
     */
    @Nullable Order fullFill(ItemPromise promise, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info);

    /**
     * Adds what the module can provide to {@code items}, leaving out what {@code filters} block.
     */
    void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters);
}
