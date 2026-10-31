package logisticspipes.request.order;

import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.promise.FluidPromise;
import logisticspipes.utils.FluidIdentifier;

public class FluidOrderManager extends OrderManager<FluidOrder, FluidIdentifier> {

	private static class IC implements OrderQueue.IIdentityProvider<FluidOrder, FluidIdentifier> {

		@Override
		public FluidIdentifier getIdentity(FluidOrder o) {
			return o.getFluid();
		}

		@Override
		public boolean isExtra(FluidOrder o) {
			return false;
		}
	}

	public FluidOrderManager(ILPPositionProvider pos) {
		super(new OrderQueue<>(new IC()), pos);
	}

	public FluidOrderManager(IChangeListener listener, ILPPositionProvider pos) {
		super(listener, pos, new OrderQueue<>(new IC()));
	}

	@Override
	public void sendFailed() {
		orders.getFirst().sendFailed();
		super.sendFailed();
	}

	public FluidOrder addOrder(FluidPromise promise, IRequestFluid destination, ResourceType type, IAdditionalTargetInformation info) {
		if (promise.amount < 0) {
			throw new RuntimeException("The amount can't be less than zero");
		}
		FluidOrder order = new FluidOrder(promise.liquid, promise.amount, destination, type, info);
		orders.addLast(order);
		listen();
		return order;
	}

	public Integer totalFluidsCountInOrders(FluidIdentifier fluid) {
		int itemCount = 0;
		for (FluidOrder request : orders) {
			if (!request.getFluid().equals(fluid)) {
				continue;
			}
			itemCount += request.getAmount();
		}
		return itemCount;
	}
}
