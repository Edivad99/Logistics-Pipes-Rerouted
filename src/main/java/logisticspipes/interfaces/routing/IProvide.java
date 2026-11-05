package logisticspipes.interfaces.routing;

import java.util.List;

import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.routing.IRouter;

/**
 * Something on the network that can answer a request.
 */
public interface IProvide {

    /**
     * Adds to {@code tree} what this can offer for it, leaving out what {@code filters} block.
     */
    void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters);

    IRouter getRouter();
}
