/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.network.to_client.pipe.PipeOrdersMessage;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

public abstract class OrderManager<T extends Order, I> implements Iterable<T> {

	protected final OrderQueue<T, I> orders;
	protected @Nullable IChangeListener listener = null;
	protected PlayerCollectionList watchingPlayers = new PlayerCollectionList();
	private final ILPPositionProvider pos;

	public OrderManager(OrderQueue<T, I> orders, ILPPositionProvider pos) {
		this.orders = orders;
		this.pos = pos;
	}

	public OrderManager(IChangeListener listener, ILPPositionProvider pos, OrderQueue<T, I> orders) {
		this(orders, pos);
		this.listener = listener;
	}

	private static void addToList(ItemIdentifierStack stack, LinkedList<ItemIdentifierStack> list) {
		for (ItemIdentifierStack ident : list) {
			if (ident.getItem().equals(stack.getItem())) {
				ident.setStackSize(ident.getStackSize() + stack.getStackSize());
				return;
			}
		}
		list.addLast(new ItemIdentifierStack(stack));
	}

	protected void listen() {
		changed();
		if (listener != null) {
			listener.listenedChanged();
		}
	}

	public void dump(StringBuilder sb) {
		for (T s : orders) {
			sb.append(s.getAsDisplayItem())
					.append(" / ")
					.append(s.getAmount())
					.append(" / ")
					.append(s.getType().name())
					.append('\n');
		}
	}

	public LinkedList<ItemIdentifierStack> getContentList(Level level) {
		if (level.isClientSide() || orders.isEmpty()) {
			return new LinkedList<>();
		}
		LinkedList<ItemIdentifierStack> list = new LinkedList<>();
		for (Order request : orders) {
			OrderManager.addToList(request.getAsDisplayItem(), list);
		}
		return list;
	}

	public boolean hasOrders(ResourceType... type) {
		return peekAtTopRequest(type) != null;
	}

	/* only multi-access SAFE when type is null; all other access patterns may change the state of the stack so the returned element is on top*/
	@SuppressWarnings("unchecked")
	public @Nullable T peekAtTopRequest(ResourceType... type) {
		List<ResourceType> typeList = Arrays.asList(type);
		if (orders.isEmpty()) {
			return null;
		}
		T top = (T) orders.getFirst().setInProgress(true);
		int loopCount = 0;
		while (!typeList.contains(top.getType())) {
			loopCount++;
			if (loopCount > orders.size()) {
				return null;
			}
			deferSend(); // sets the new top to InProgress
			top = orders.getFirst();
		}
		return top;
	}

	@SuppressWarnings("unchecked")
	public void sendSuccessfull(int number, boolean defersend, @Nullable IRoutedItem item) {
		orders.getFirst().reduceAmountBy(number);
		if (orders.getFirst().isWatched() && item != null) {
			IDistanceTracker tracker = new DistanceTracker();
			item.setDistanceTracker(tracker);
			orders.getFirst().addDistanceTracker(tracker);
		}
		int destination = orders.getFirst().getRouterId();
		if (orders.getFirst().getAmount() <= 0) {
			Order order = orders.removeFirst();
			order.setFinished(true);
			order.setInProgress(false);
		}
		if (!orders.isEmpty()) {
			Order start = orders.getFirst();
			if (defersend && destination == start.getRouterId()) {
				orders.addLast((T) orders.removeFirst().setInProgress(false));
				while (start != orders.getFirst() && destination == orders.getFirst().getRouterId()) {
					orders.addLast(orders.removeFirst());
				}
				if (start == orders.getFirst()) {
					orders.addLast(orders.removeFirst());
				}
				orders.getFirst().setInProgress(true);
			}
		}
		listen();
	}

	public void sendFailed() {
		if (!orders.isEmpty()) {
			Order order = orders.removeFirst();
			order.setFinished(true);
			order.setInProgress(false);
		}
		if (!orders.isEmpty()) {
			orders.getFirst().setInProgress(true);
		}
		listen();
	}

	@SuppressWarnings("unchecked")
	public void deferSend() {
		orders.addLast((T) orders.removeFirst().setInProgress(false));
		orders.getFirst().setInProgress(true);
		listen();
	}

	public int totalAmountCountInAllOrders() {
		int amount = 0;
		for (Order request : orders) {
			amount += request.getAmount();
		}
		return amount;
	}

	public void setMachineProgress(byte progress) {
		if (orders.isEmpty()) {
			return;
		}
		orders.getFirst().setMachineProgress(progress);
		changed();
	}

	public boolean isFirstOrderWatched() {
		if (orders.isEmpty()) {
			return false;
		}
		return orders.getFirst().isWatched();
	}

	private PipeOrdersMessage contentMessage() {
		final List<IOrderInfoProvider> content = new ArrayList<>();
		orders.forEach(content::add);
		return new PipeOrdersMessage(pos.getPos(), content);
	}

	public void startWatching(Player player) {
		watchingPlayers.add(player);
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, contentMessage());
		}
	}

	public void stopWatching(Player player) {
		watchingPlayers.remove(player);
	}

	public boolean hasExtras() {
		return orders.hasExtras();
	}

	private void changed() {
		if (watchingPlayers.isEmpty()) {
			return;
		}
		//if(!oldOrders.equals(_orders)) {
		//	oldOrders.clear();
		//	oldOrders.addAll(_orders);
		watchingPlayers.send(contentMessage());
		//}
	}

	/**
	 * DON'T MODIFY TROUGH THIS ONLY READ THE VALUES
	 */
	@Override
	public Iterator<T> iterator() {
		return this.orders.iterator();
	}

	public int size() {
		return orders.size();
	}
}
