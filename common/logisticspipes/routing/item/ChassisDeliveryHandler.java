package logisticspipes.routing.item;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ChassisDeliveryHandler extends DeliveryHandler {

	private final PipeLogisticsChassis chassisPipe;

	public ChassisDeliveryHandler(PipeLogisticsChassis chassisPipe) {
		this.chassisPipe = chassisPipe;
	}

	@Override
	public @Nullable Direction itemArrived(IRoutedItem item, @Nullable Direction denied) {
		if (item.getItemIdentifierStack() != null) {
			chassisPipe.receivedItem(item.getItemIdentifierStack().getStackSize());
		}
		return chassisPipe.getPointedOrientation();
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		LogisticsModule module = chassisPipe.getLogisticsModule();
		if (module == null) {
			chassisPipe.notifyOfItemArrival(item.getInfo());
			return false;
		}
		if (!chassisPipe.isEnabled()) {
			chassisPipe.notifyOfItemArrival(item.getInfo());
			return false;
		}
		final ItemIdentifierStack itemIdStack = item.getItemIdentifierStack();
		SinkReply reply = module.sinksItem(itemIdStack.makeNormalStack(), itemIdStack.getItem(), -1, 0, true, false, false);
		if (reply == null || reply.maxNumberOfItems < 0) {
			chassisPipe.notifyOfItemArrival(item.getInfo());
			return false;
		}

		if (reply.maxNumberOfItems > 0 && itemIdStack.getStackSize() > reply.maxNumberOfItems) {
			Direction o = chassisPipe.getPointedOrientation();
			if (o == null) {
				o = Direction.UP;
			}

			item.split(reply.maxNumberOfItems, o);
		}
		return true;
	}

}
