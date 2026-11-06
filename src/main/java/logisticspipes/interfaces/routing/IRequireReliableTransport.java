package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A pipe that keeps count of the items on their way to it, and so has to hear how that ends.
 */
public interface IRequireReliableTransport {

    void itemLost(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info);

    void itemArrived(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info);
}
