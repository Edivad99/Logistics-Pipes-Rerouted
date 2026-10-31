package logisticspipes.request.order;

import lombok.Getter;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidOrder extends Order {

	public FluidOrder(FluidIdentifier fluid, Integer amount, IRequestFluid destination, ResourceType type,
			@Nullable IAdditionalTargetInformation info) {
		super(type, info);
		this.fluid = fluid;
		this.amount = amount;
		this.destination = destination;
	}

	@Getter
	private final FluidIdentifier fluid;
	@Getter
	private int amount;
	private final IRequestFluid destination;

	@Override
	public ItemIdentifierStack getAsDisplayItem() {
		return fluid.getItemIdentifier().makeStack(amount);
	}

	@Override
    public IRouter getRouter() {
		return destination.getRouter();
	}

	@Override
	public void sendFailed() {
		destination.sendFailed(fluid, amount);
	}

	@Override
	public void reduceAmountBy(int reduce) {
		amount -= reduce;
	}
}
