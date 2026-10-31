package logisticspipes.request.promise;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.request.resources.DictResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.utils.item.ItemIdentifierStack;

public class DictPromise extends ItemPromise {

	@Getter
	private DictResource resource;

	public DictPromise(DictResource item, int stackSize, IProvideItems sender, IOrderInfoProvider.@Nullable ResourceType type) {
		super(item.stack.getItem(), stackSize, sender, type);
		this.resource = item;
		this.resource.stack = new ItemIdentifierStack(this.resource.stack);
		this.resource.stack.setStackSize(stackSize);
	}

	@Override
	public IExtraPromise split(int more) {
		numberOfItems -= more;
		this.resource.stack.setStackSize(numberOfItems);
		return new ExtraDictPromise(getResource().clone(), more, sender, false);
	}
}
