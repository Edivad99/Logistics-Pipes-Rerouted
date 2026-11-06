package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A requester of items.
 */
public interface IRequestItems extends Comparable<IRequestItems>, IRequest {

    /**
     * An order for this pipe could not be sent.
     */
    void itemCouldNotBeSent(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info);

    @Override
    int compareTo(IRequestItems other);
}
