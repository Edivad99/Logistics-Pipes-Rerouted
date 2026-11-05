package logisticspipes.interfaces.routing;

import logisticspipes.routing.item.ItemRoutingInformation;

/**
 * A pipe that links to others over a channel rather than through pipes.
 */
public interface IChannelRoutingConnection {

    /**
     * @return what the link costs the route finder
     */
    int getConnectionResistance();

    /**
     * Notes an item as on its way through the link.
     */
    void addItem(ItemRoutingInformation info);
}
