package logisticspipes.request.order;

import lombok.Getter;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.resources.DictResource;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemOrder extends Order {

	public ItemOrder(DictResource item, @Nullable IRequestItems destination, ResourceType type,
			@Nullable IAdditionalTargetInformation info) {
		super(type, info);
		resource = item;
		this.destination = destination;
	}

	@Getter
	private final DictResource resource;
	@Getter
	private final @Nullable IRequestItems destination;

	@Override
	public @Nullable IRouter getRouter() {
		if (destination == null) {
			return null;
		}
		return destination.getRouter();
	}

	@Override
	public void sendFailed() {
		if (destination == null) {
			return;
		}
		destination.itemCouldNotBeSend(getResource().stack, getInformation());
	}

	@Override
	public ItemIdentifierStack getAsDisplayItem() {
		return resource.stack;
	}

	@Override
	public int getAmount() {
		return resource.stack.getStackSize();
	}

	@Override
	public void reduceAmountBy(int amount) {
		resource.stack.setStackSize(resource.stack.getStackSize() - amount);
	}
}
