/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing.item;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.routing.sink.ItemSinkFinder;

/**
 * @author Krapht This class is responsible for resolving where incoming items
 * should go.
 */
public class RouteResolver {

    protected final IRouter router;
    private final DeliveryHandler deliveryHandler;
    private final CoreRoutedPipe pipe;

    public RouteResolver(IRouter router, DeliveryHandler deliveryHandler, CoreRoutedPipe pipe) {
        this.router = router;
        this.deliveryHandler = deliveryHandler;
        this.pipe = pipe;
    }

    public @Nullable Direction getOrientationForItem(IRoutedItem item, @Nullable Direction blocked) {

        item.checkIDFromUUID();
        //If a item has no destination, find one
        if (item.getDestination() < 0) {
            item = ItemSinkFinder.assignDestinationFor(item, router.getSimpleID(), false);
            pipe.debug.log("No Destination, assigned new destination: (" + item.getInfo());
        }

        //If the destination is unknown / unroutable or it already arrived at its destination and somehow looped back
        if (item.getDestination() >= 0 && (
            !router.hasRoute(item.getDestination(), item.getTransportMode() == TransportMode.Active,
                item.getItemIdentifierStack().getItem()) || item.getArrived())) {
            item = ItemSinkFinder.assignDestinationFor(item, router.getSimpleID(), false);
            pipe.debug.log("Unreachable Destination, assigned new destination: (" + item.getInfo());
        }

        item.checkIDFromUUID();
        //If we still have no destination or client side unroutable, drop it
        if (item.getDestination() < 0) {
            return null;
        }

        // Are we the destination? Deliver it
        if (router.getId().equals(item.getDestinationUUID())) {

            deliveryHandler.handleItem(item);

            if (item.getDistanceTracker() != null) {
                item.getDistanceTracker().setCurrentDistanceToTarget(0);
                item.getDistanceTracker().setDestinationReached();
            }

            if (item.getTransportMode() != TransportMode.Active && !deliveryHandler.stillWantItem(item)) {
                return getOrientationForItem(ItemSinkFinder.assignDestinationFor(item, router.getSimpleID(), true),
                    null);
            }

            item.setDoNotBuffer(true);
            item.setArrived(true);
            return deliveryHandler.itemArrived(item, blocked);
        }

        //Do we now know the destination?
        if (!router.hasRoute(item.getDestination(), item.getTransportMode() == TransportMode.Active,
            item.getItemIdentifierStack().getItem())) {
            return null;
        }

        //Which direction should we send it
        ExitRoute exit = router.getExitFor(item.getDestination(), item.getTransportMode() == TransportMode.Active,
            item.getItemIdentifierStack().getItem());
        if (exit == null) {
            return null;
        }

        if (item.getDistanceTracker() != null) {
            item.getDistanceTracker().setCurrentDistanceToTarget(exit.blockDistance);
        }

        return exit.exitOrientation;
    }
}
