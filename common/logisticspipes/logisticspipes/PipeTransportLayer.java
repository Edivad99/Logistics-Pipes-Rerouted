package logisticspipes.logisticspipes;

import java.util.LinkedList;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

/**
 * This class is responsible for handling incoming items for standard pipes
 *
 * @author Krapht
 */
public class PipeTransportLayer extends TransportLayer {

	private final CoreRoutedPipe routedPipe;
	private final ITrackStatistics trackStatistics;
	private final IRouter router;

	public PipeTransportLayer(CoreRoutedPipe routedPipe, ITrackStatistics trackStatistics, IRouter router) {
		this.routedPipe = routedPipe;
		this.trackStatistics = trackStatistics;
		this.router = router;
	}

	@Override
	public Direction itemArrived(IRoutedItem item, Direction denied) {
		if (item.getItemIdentifierStack() != null) {
			trackStatistics.receivedItem(item.getItemIdentifierStack().getStackSize());
		}

		// 1st priority, deliver to adjacent inventories
		LinkedList<Direction> possibleDirection = new LinkedList<>();
		for (NeighborTileEntity<BlockEntity> adjacent : routedPipe.getAvailableAdjacent().inventories()) {
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
		if (possibleDirection.size() != 0) {
			return possibleDirection.get(routedPipe.getWorld().getRandom().nextInt(possibleDirection.size()));
		}

		// 2nd priority, deliver to non-routed exit
		new WorldCoordinatesWrapper(routedPipe.container).connectedTileEntities().stream()
				.filter(neighbor -> {
					if (router.isRoutedExit(neighbor.getDirection())) return false;
					final CoreRoutedPipe routerPipe = router.getPipe();
					return routerPipe == null || !routerPipe.isLockedExit(neighbor.getDirection());
				})
				.forEach(neighbor -> possibleDirection.add(neighbor.getDirection()));

		if (possibleDirection.size() == 0) {
			// last resort, drop item
			return null;
		} else {
			return possibleDirection.get(routedPipe.getWorld().getRandom().nextInt(possibleDirection.size()));
		}
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		// pipes are dumb and always want the item
		return true;
	}

}
