package logisticspipes.routing;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.request.IExtraPromise;
import logisticspipes.request.resources.DictResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsDictPromise extends LogisticsPromise {

	@Getter
	private DictResource resource;

	public LogisticsDictPromise(DictResource item, int stackSize, IProvideItems sender, IOrderInfoProvider.@Nullable ResourceType type) {
		super(item.stack.getItem(), stackSize, sender, type);
		this.resource = item;
		this.resource.stack = new ItemIdentifierStack(this.resource.stack);
		this.resource.stack.setStackSize(stackSize);
	}

	@Override
	public IExtraPromise split(int more) {
		numberOfItems -= more;
		this.resource.stack.setStackSize(numberOfItems);
		return new LogisticsExtraDictPromise(getResource().clone(), more, sender, false);
	}
}
