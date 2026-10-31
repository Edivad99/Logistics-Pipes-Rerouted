package logisticspipes.request.promise;

import lombok.Getter;

import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;

public class ExtraDictPromise extends DictPromise implements IExtraPromise {

	public ExtraDictPromise(DictResource item, int numberOfItems, IProvideItems sender, boolean provided) {
		super(item, numberOfItems, sender, null);
		this.provided = provided;
	}

	@Getter
	public boolean provided;

	@Override
	public ExtraDictPromise copy() {
		return new ExtraDictPromise(getResource(), numberOfItems, sender, provided);
	}

	@Override
	public void registerExtras(IResource requestType) {
		if (sender instanceof ICraftItems) {
			((ICraftItems) sender).registerExtras(this);
		}
	}

	@Override
	public void lowerAmount(int usedcount) {
		numberOfItems -= usedcount;
	}

	@Override
	public void setAmount(int amount) {
		numberOfItems = amount;
	}
}
