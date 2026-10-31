package logisticspipes.routing.item;

import java.util.LinkedList;
import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.connection.LPNeighborBlockEntity;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;

/**
 * This class is responsible for handling incoming items for standard pipes
 *
 * @author Krapht
 */
public class PipeDeliveryHandler extends DeliveryHandler {

    private final CoreRoutedPipe routedPipe;
    private final IRouter router;

    public PipeDeliveryHandler(CoreRoutedPipe routedPipe, IRouter router) {
        this.routedPipe = routedPipe;
        this.router = router;
    }

    @Override
    public @Nullable Direction itemArrived(IRoutedItem item, @Nullable Direction denied) {
        if (item.getItemIdentifierStack() != null) {
            routedPipe.receivedItem(item.getItemIdentifierStack().getStackSize());
        }

        // 1st priority, deliver to adjacent inventories
        LinkedList<Direction> possibleDirection = new LinkedList<>();
        for (NeighborBlockEntity<BlockEntity> adjacent : routedPipe.getAvailableAdjacent().inventories()) {
            if (router.isRoutedExit(adjacent.getDirection())) {
                continue;
            }
            if (denied != null && denied.equals(adjacent.getDirection())) {
                continue;
            }

            CoreRoutedPipe pipe = router.getPipe();
            if (pipe != null) {
                if (pipe.isLockedExit(adjacent.getDirection())) {
                    continue;
                }
            }

            possibleDirection.add(adjacent.getDirection());
        }
        // An item only arrives at a pipe that is in a level.
        final RandomSource random = Objects.requireNonNull(routedPipe.getLevel()).getRandom();
        if (!possibleDirection.isEmpty()) {
            return possibleDirection.get(random.nextInt(possibleDirection.size()));
        }

        // 2nd priority, deliver to non-routed exit
        LPNeighborBlockEntity.connectedTo(Objects.requireNonNull(routedPipe.getContainer())).stream()
            .filter(neighbor -> {
                if (router.isRoutedExit(neighbor.getDirection())) {
                    return false;
                }
                final CoreRoutedPipe routerPipe = router.getPipe();
                return routerPipe == null || !routerPipe.isLockedExit(neighbor.getDirection());
            })
            .forEach(neighbor -> possibleDirection.add(neighbor.getDirection()));

        if (possibleDirection.isEmpty()) {
            // last resort, drop item
            return null;
        } else {
            return possibleDirection.get(random.nextInt(possibleDirection.size()));
        }
    }

    @Override
    public boolean stillWantItem(IRoutedItem item) {
        // pipes are dumb and always want the item
        return true;
    }

}
