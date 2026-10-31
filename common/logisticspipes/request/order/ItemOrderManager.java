package logisticspipes.request.order;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.resources.DictResource;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemOrderManager extends OrderManager<ItemOrder, DictResource.Identifier> {

	private static class IC implements OrderQueue.IIdentityProvider<ItemOrder, DictResource.Identifier> {

		@Override
		public DictResource.Identifier getIdentity(ItemOrder o) {
			if (o == null || o.getResource() == null) {
				return null;
			}
			return o.getResource().getIdentifier();
		}

		@Override
		public boolean isExtra(ItemOrder o) {
			return o instanceof ExtraItemOrder;
		}
	}

	private static class ExtraItemOrder extends ItemOrder {

		public ExtraItemOrder(DictResource item, @Nullable IRequestItems destination, ResourceType type,
				@Nullable IAdditionalTargetInformation info) {
			super(item, destination, type, info);
		}
	}

	public ItemOrderManager(ILPPositionProvider pos) {
		super(new OrderQueue<>(new IC()), pos);
	}

	public ItemOrderManager(IChangeListener listener, ILPPositionProvider pos) {
		super(listener, pos, new OrderQueue<>(new IC()));
	}

	@Override
	public void sendFailed() {
		orders.getFirst().sendFailed();
		super.sendFailed();
	}

	public ItemOrder addOrder(ItemIdentifierStack stack, IRequestItems requester, ResourceType type,
			@Nullable IAdditionalTargetInformation info) {
		ItemOrder order = new ItemOrder(new DictResource(stack, null), requester, type, info);
		orders.addLast(order);
		listen();
		return order;
	}

	public ItemOrder addOrder(DictResource stack, IRequestItems requester, ResourceType type,
			@Nullable IAdditionalTargetInformation info) {
		ItemOrder order = new ItemOrder(stack, requester, type, info);
		orders.addLast(order);
		listen();
		return order;
	}

	public ExtraItemOrder addExtra(DictResource stack) {
		ExtraItemOrder order = new ExtraItemOrder(stack, null, ResourceType.EXTRA, null);
		orders.addLast(order);
		listen();
		return order;
	}

	public void removeExtras(DictResource resource) {
		int itemsToRemove = resource.getRequestedAmount();
		DictResource.Identifier ident = resource.getIdentifier();
		Iterator<ItemOrder> iter = orders.iterator();
		List<ItemOrder> toRemove = new LinkedList<>();
		while (iter.hasNext()) {
			ItemOrder order = iter.next();
			if (order.getType() != ResourceType.EXTRA) continue;
			if (order.getResource().getIdentifier().equals(ident)) {
				if (itemsToRemove >= order.getAmount()) {
					itemsToRemove -= order.getAmount();
					toRemove.add(order);
					if (itemsToRemove == 0) {
						orders.removeAll(toRemove);
						return;
					}
				} else {
					order.getResource().getItemStack().setStackSize(order.getAmount() - itemsToRemove);
					break;
				}
			}
		}
		orders.removeAll(toRemove);
	}

	public int totalItemsCountInOrders(ItemIdentifier item) {
		int itemCount = 0;
		for (ItemOrder request : orders) {
			if (!request.getResource().getItem().equals(item)) {
				continue;
			}
			itemCount += request.getResource().stack.getStackSize();
		}
		return itemCount;
	}
}
