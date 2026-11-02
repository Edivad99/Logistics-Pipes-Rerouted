package logisticspipes.request.order;

import org.jspecify.annotations.Nullable;

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

    @Override
    public void sendFailed() {
        orders.getFirst().sendFailed();
        super.sendFailed();
    }

    public FluidOrder addOrder(FluidPromise promise, IRequestFluid destination, ResourceType type,
        @Nullable IAdditionalTargetInformation info) {
        if (promise.amount < 0) {
            throw new IllegalArgumentException("The amount can't be less than zero");
        }
        FluidOrder order = new FluidOrder(promise.liquid, promise.amount, destination, type, info);
        orders.addLast(order);
        listen();
        return order;
    }

    public int totalFluidsCountInOrders(FluidIdentifier fluid) {
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
