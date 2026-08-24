/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logisticspipes;

import net.minecraft.core.Direction;

import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;

/**
 * @author Krapht This class is responsible for resolving where incoming items
 * should go.
 */
public class RouteLayer {

	protected final IRouter router;
	private final TransportLayer transport;
	private final CoreRoutedPipe pipe;

	public RouteLayer(IRouter router, TransportLayer transportLayer, CoreRoutedPipe pipe) {
		this.router = router;
		transport = transportLayer;
		this.pipe = pipe;
	}

	public Direction getOrientationForItem(IRoutedItem item, Direction blocked) {

		item.checkIDFromUUID();
		//If a item has no destination, find one
		if (item.getDestination() < 0) {
			item = SimpleServiceLocator.logisticsManager.assignDestinationFor(item, router.getSimpleID(), false);
			pipe.debug.log("No Destination, assigned new destination: (" + item.getInfo());
		}

		//If the destination is unknown / unroutable or it already arrived at its destination and somehow looped back
		if (item.getDestination() >= 0 && (!router.hasRoute(item.getDestination(), item.getTransportMode() == TransportMode.Active, item.getItemIdentifierStack().getItem()) || item.getArrived())) {
			item = SimpleServiceLocator.logisticsManager.assignDestinationFor(item, router.getSimpleID(), false);
			pipe.debug.log("Unreachable Destination, assigned new destination: (" + item.getInfo());
		}

		item.checkIDFromUUID();
		//If we still have no destination or client side unroutable, drop it
		if (item.getDestination() < 0) {
			return null;
		}

		// Are we the destination? Deliver it
		if (item.getDestinationUUID().equals(router.getId())) {

			transport.handleItem(item);

			if (item.getDistanceTracker() != null) {
				item.getDistanceTracker().setCurrentDistanceToTarget(0);
				item.getDistanceTracker().setDestinationReached();
			}

			if (item.getTransportMode() != TransportMode.Active && !transport.stillWantItem(item)) {
				return getOrientationForItem(SimpleServiceLocator.logisticsManager.assignDestinationFor(item, router.getSimpleID(), true), null);
			}

			item.setDoNotBuffer(true);
			item.setArrived(true);
			return transport.itemArrived(item, blocked);
		}

		//Do we now know the destination?
		if (!router.hasRoute(item.getDestination(), item.getTransportMode() == TransportMode.Active, item.getItemIdentifierStack().getItem())) {
			return null;
		}

		//Which direction should we send it
		ExitRoute exit = router.getExitFor(item.getDestination(), item.getTransportMode() == TransportMode.Active, item.getItemIdentifierStack().getItem());
		if (exit == null) {
			return null;
		}

		if (item.getDistanceTracker() != null) {
			item.getDistanceTracker().setCurrentDistanceToTarget(exit.blockDistance);
		}

		return exit.exitOrientation;
	}
}
