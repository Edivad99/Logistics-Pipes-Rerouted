package logisticspipes.interfaces;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.OrderTree;
import logisticspipes.request.resources.IResource;

/**
 * A pipe that keeps track of the requests it placed, for the crafting monitor on its screen.
 */
public interface IRequestWatcher {

    /**
     * A request was placed, on the server.
     *
     * @param resource what was requested, or null when the request was for several things at once
     */
    void handleOrderList(@Nullable IResource resource, OrderTree orders);

    /**
     * The server says it is watching a request, under {@code watcherId}.
     */
    void handleClientSideListInfo(int watcherId, @Nullable IResource resource, OrderTree orders);

    /**
     * The server stopped watching a request.
     *
     * @param watcherId the request's id, or -1 for all of them
     */
    void handleClientSideRemove(int watcherId);
}
