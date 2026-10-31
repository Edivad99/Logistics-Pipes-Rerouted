package logisticspipes.request.promise;

import lombok.Getter;

import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.item.ItemIdentifier;

public class ExtraItemPromise extends ItemPromise implements IExtraPromise {

	public ExtraItemPromise(ItemIdentifier item, int numberOfItems, IProvideItems sender, boolean provided) {
		super(item, numberOfItems, sender, null);
		this.provided = provided;
	}

	@Getter
	public boolean provided;

	@Override
	public ExtraItemPromise copy() {
		return new ExtraItemPromise(item, numberOfItems, sender, provided);
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
