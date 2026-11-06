package logisticspipes.interfaces.routing;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.Order;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * A provider of items.
 */
public interface IProvideItems extends IProvide {

    /**
     * @return the order placed, or null when none could be
     */
    @Nullable Order fullFill(ItemPromise promise, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info);

    /**
     * Adds what this can provide to {@code items}, leaving out what {@code filters} block.
     */
    void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters);
}
