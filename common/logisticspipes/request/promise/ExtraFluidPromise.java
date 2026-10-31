package logisticspipes.request.promise;

import lombok.Getter;

import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.FluidIdentifier;

public class ExtraFluidPromise extends FluidPromise implements IExtraPromise {

	@Getter
	private boolean provided;

	public ExtraFluidPromise(FluidIdentifier liquid, int amount, IProvideFluids sender, boolean provided) {
		this.liquid = liquid;
		this.amount = amount;
		this.sender = sender;
		this.type = null; // extra promises carry no ResourceType, consistent with ExtraItemPromise
		this.provided = provided;
	}

	@Override
	public ExtraFluidPromise copy() {
		return new ExtraFluidPromise(liquid, amount, sender, provided);
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
