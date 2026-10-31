package logisticspipes.interfaces.routing;

import logisticspipes.routing.item.ItemRoutingInformation;

public interface IChannelRoutingConnection {

	int getConnectionResistance();

	void addItem(ItemRoutingInformation info);
}
