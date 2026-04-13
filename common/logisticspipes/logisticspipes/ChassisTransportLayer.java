package logisticspipes.logisticspipes;

import net.minecraft.core.Direction;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ChassisTransportLayer extends TransportLayer {

	private final PipeLogisticsChassis _chassisPipe;

	public ChassisTransportLayer(PipeLogisticsChassis chassisPipe) {
		_chassisPipe = chassisPipe;
	}

	@Override
	public Direction itemArrived(IRoutedItem item, Direction denied) {
		if (item.getItemIdentifierStack() != null) {
			_chassisPipe.receivedItem(item.getItemIdentifierStack().getStackSize());
		}
		return _chassisPipe.getPointedOrientation();
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		LogisticsModule module = _chassisPipe.getLogisticsModule();
		if (module == null) {
			_chassisPipe.notifyOfItemArival(item.getInfo());
			return false;
		}
		if (!_chassisPipe.isEnabled()) {
			_chassisPipe.notifyOfItemArival(item.getInfo());
			return false;
		}
		final ItemIdentifierStack itemIdStack = item.getItemIdentifierStack();
		SinkReply reply = module.sinksItem(itemIdStack.makeNormalStack(), itemIdStack.getItem(), -1, 0, true, false, false);
		if (reply == null || reply.maxNumberOfItems < 0) {
			_chassisPipe.notifyOfItemArival(item.getInfo());
			return false;
		}

		if (reply.maxNumberOfItems > 0 && itemIdStack.getStackSize() > reply.maxNumberOfItems) {
			Direction o = _chassisPipe.getPointedOrientation();
			if (o == null) {
				o = Direction.UP;
			}

			item.split(reply.maxNumberOfItems, o);
		}
		return true;
	}

}
