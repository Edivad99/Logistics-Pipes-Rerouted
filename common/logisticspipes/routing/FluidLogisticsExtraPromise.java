package logisticspipes.routing;

import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.request.IExtraPromise;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.FluidIdentifier;
import lombok.Getter;

public class FluidLogisticsExtraPromise extends FluidLogisticsPromise implements IExtraPromise {

	@Getter
	private boolean provided;

	public FluidLogisticsExtraPromise(FluidIdentifier liquid, int amount, IProvideFluids sender, boolean provided) {
		this.liquid = liquid;
		this.amount = amount;
		this.sender = sender;
		this.type = null; // extra promises carry no ResourceType, consistent with LogisticsExtraPromise
		this.provided = provided;
	}

	@Override
	public FluidLogisticsExtraPromise copy() {
		return new FluidLogisticsExtraPromise(liquid, amount, sender, provided);
	}

	@Override
	public void registerExtras(IResource requestType) {
		// IProvideFluids does not implement ICraft; no crafting extras to register.
	}

	@Override
	public void lowerAmount(int usedcount) {
		amount -= usedcount;
	}

	@Override
	public void setAmount(int amount) {
		this.amount = amount;
	}
}
