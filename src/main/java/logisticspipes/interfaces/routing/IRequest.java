package logisticspipes.interfaces.routing;

import logisticspipes.routing.IRouter;

/**
 * Something on the network that requests resources.
 */
public interface IRequest {

    IRouter getRouter();

    int getID();
}
