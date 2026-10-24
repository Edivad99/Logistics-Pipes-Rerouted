package logisticspipes.interfaces.routing;

import java.util.List;

/**
 * Something reached over the network, with the filters on the way to it: a firewall on the route
 * can still block what the target would otherwise offer.
 */
public record FilteredRoute<T>(T target, List<IFilter> filters) {}
