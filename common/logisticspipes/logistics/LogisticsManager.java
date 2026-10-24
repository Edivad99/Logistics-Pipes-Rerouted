/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logistics;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.item.LogisticsFluidContainer;

public final class LogisticsManager {

	private LogisticsManager() {
	}

	/** The router an item is best sent to, by simple id, and what it answered. */
	private record BestReply(int routerId, SinkReply reply) {}

	private static @Nullable BestReply getBestReply(ItemStack stack, ItemIdentifier item, @Nullable IRouter sourceRouter,
			List<ExitRoute> validDestinations, boolean excludeSource, List<Integer> jamList, boolean allowDefault) {
		BestReply best = null;

		outer:
		for (ExitRoute candidateRouter : validDestinations) {
			if (excludeSource) {
				if (candidateRouter.destination.getId().equals(sourceRouter.getId())) {
					continue;
				}
			}
			if (jamList.contains(candidateRouter.destination.getSimpleID())) {
				continue;
			}

			if (!candidateRouter.containsFlag(PipeRoutingConnectionType.CAN_ROUTE_TO)) {
				continue;
			}

			for (IFilter filter : candidateRouter.filters) {
				if (filter.blockRouting() || (filter.isBlocked() == filter.isFilteredItem(item))) {
					continue outer;
				}
			}

			SinkReply reply = LogisticsManager.canSink(stack, candidateRouter.destination, sourceRouter, excludeSource, item, best == null ? null : best.reply(), false, allowDefault);

			if (reply == null) {
				continue;
			}
			if (best == null
					|| reply.fixedPriority.ordinal() > best.reply().fixedPriority.ordinal()
					|| reply.fixedPriority == best.reply().fixedPriority && reply.customPriority > best.reply().customPriority) {
				best = new BestReply(candidateRouter.destination.getSimpleID(), reply);
			}
		}
		if (best != null) {
			CoreRoutedPipe pipe = RouterManager.get().getServerRouter(best.routerId()).getPipe();
			pipe.useEnergy(best.reply().energyUse);
			pipe.spawnParticle(Particles.BLUE_SPARKLE, 10);
		}
		return best;
	}

	public static @Nullable SinkReply canSink(ItemStack stack, IRouter destination, @Nullable IRouter sourceRouter, boolean excludeSource, ItemIdentifier item,
			@Nullable SinkReply result, boolean activeRequest, boolean allowDefault) {
		return canSink(stack, destination, sourceRouter, excludeSource, item, result, activeRequest, allowDefault, true);
	}

	public static @Nullable SinkReply canSink(ItemStack stack, IRouter destination, @Nullable IRouter sourceRouter, boolean excludeSource, ItemIdentifier item,
			@Nullable SinkReply result, boolean activeRequest, boolean allowDefault, boolean forcePassive) {

		SinkReply reply;
		LogisticsModule module = destination.getLogisticsModule();
		CoreRoutedPipe crp = destination.getPipe();
		if (module == null) {
			return null;
		}
		if (!(module.receivePassive() || activeRequest)) {
			return null;
		}
		if (crp == null || !crp.isEnabled()) {
			return null;
		}
		if (excludeSource && sourceRouter != null) {
			if (destination.getPipe().isOnSameContainer(sourceRouter.getPipe())) {
				return null;
			}
		}
		if (result == null) {
			reply = module.sinksItem(stack, item, -1, 0, allowDefault, true, forcePassive);
		} else {
			reply = module.sinksItem(stack, item, result.fixedPriority.ordinal(), result.customPriority, allowDefault, true, forcePassive);
		}
		if (result != null && result.maxNumberOfItems < 0) {
			return null;
		}
		return reply;
	}

	/**
	 * Will assign a destination for a IRoutedItem based on the best sink reply
	 * received from other pipes.
	 *
	 * @param item
	 *            The item that needs to be routed.
	 * @param sourceRouterID
	 *            The SimpleID of the pipe that is sending the item. (the
	 *            routedItem will cache the UUID, and that the SimpleID belongs
	 *            to the UUID will be checked when appropriate)
	 * @param excludeSource
	 *            Boolean, true means that it won't set the source as the
	 *            destination.
	 * @return IRoutedItem with a newly assigned destination
	 */
	public static IRoutedItem assignDestinationFor(IRoutedItem item, int sourceRouterID, boolean excludeSource) {

		//Assert: only called server side.

		//If we for some reason can't get the router we can't do anything either
		IRouter sourceRouter = RouterManager.get().getServerRouter(sourceRouterID);
		if (sourceRouter == null) {
			return item;
		}

		//Wipe current destination
		item.clearDestination();

		final ItemIdentifierStack itemIdStack = item.getItemIdentifierStack();
		if (itemIdStack == null) {
			return item;
		}
		BitSet routersIndex = ServerRouter.getRoutersInterestedIn(itemIdStack.getItem());
		List<ExitRoute> validDestinations = new ArrayList<>(); // get the routing table
		for (int i = routersIndex.nextSetBit(0); i >= 0; i = routersIndex.nextSetBit(i + 1)) {
			IRouter r = RouterManager.get().getServerRouter(i);
			validDestinations.addAll(sourceRouter.getDistanceTo(r).stream()
					.filter(e -> e.containsFlag(PipeRoutingConnectionType.CAN_ROUTE_TO))
					.collect(Collectors.toList()));
		}
		Collections.sort(validDestinations);
		final ItemStack stack = itemIdStack.makeNormalStack();
		if (stack.getItem() instanceof LogisticsFluidContainer) {
			CoreRoutedPipe sourcePipe = sourceRouter.getPipe();
			Level level = sourcePipe == null ? null : sourcePipe.getLevel();
			FluidIdentifierStack fluid = level == null ? null
					: LogisticsFluidManager.getFluidFromContainer(itemIdStack);
			if (fluid != null) {
				Pair<Integer, FluidSinkReply> bestReply = LogisticsFluidManager.getBestReply(fluid, sourceRouter, item.getJamList());
				if (bestReply != null) {
					item.setDestination(bestReply.getValue1());
				}
			}
		} else {
			BestReply bestReply = getBestReply(stack, itemIdStack.getItem(), sourceRouter, validDestinations, excludeSource, item.getJamList(), true);
			if (bestReply != null && bestReply.routerId() != 0) {
				item.setDestination(bestReply.routerId());
				if (bestReply.reply().isPassive) {
					if (bestReply.reply().isDefault) {
						item.setTransportMode(TransportMode.Default);
					} else {
						item.setTransportMode(TransportMode.Passive);
					}
				} else {
					item.setTransportMode(TransportMode.Active);
				}
				item.setAdditionalTargetInformation(bestReply.reply().addInfo);
			}
		}
		return item;
	}

	/**
	 * @param validDestinations
	 *            a list of ExitRoute of valid destinations.
	 * @return HashMap with ItemIdentifier and Integer item count of available
	 *         items.
	 */
	public static HashMap<ItemIdentifier, Integer> getAvailableItems(List<ExitRoute> validDestinations) {
		//TODO: Replace this entire function with a fetch from the pre-built arrays (path incoming later)
		List<Map<ItemIdentifier, Integer>> items = new ArrayList<>(ServerRouter.getBiggestSimpleID());
		for (int i = 0; i < ServerRouter.getBiggestSimpleID(); i++) {
			items.add(new HashMap<>());
		}
		BitSet used = new BitSet(ServerRouter.getBiggestSimpleID());
		outer:
		for (ExitRoute r : validDestinations) {
			if (r == null) {
				continue;
			}
			if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
				continue;
			}
			if (!(r.destination.getPipe() instanceof IProvideItems)) {
				continue;
			}
			for (IFilter filter : r.filters) {
				if (filter.blockProvider()) {
					continue outer;
				}
			}
			IProvideItems provider = (IProvideItems) r.destination.getPipe();
			provider.getAllItems(items.get(r.destination.getSimpleID()), r.filters);
			used.set(r.destination.getSimpleID(), true);
		}
		//TODO: Fix this doubly nested list
		HashMap<ItemIdentifier, Integer> allAvailableItems = new HashMap<>();
		for (Map<ItemIdentifier, Integer> allItems : items) {
			for (Entry<ItemIdentifier, Integer> item : allItems.entrySet()) {
				allAvailableItems.merge(item.getKey(), item.getValue(), Integer::sum);
			}
		}
		return allAvailableItems;
	}

	/**
	 * @param validDestinations
	 *            a List of ExitRoute of valid destinations.
	 * @return LinkedList with ItemIdentifier
	 */
	public static LinkedList<ItemIdentifier> getCraftableItems(List<ExitRoute> validDestinations) {
		LinkedList<ItemIdentifier> craftableItems = new LinkedList<>();
		BitSet used = new BitSet(ServerRouter.getBiggestSimpleID());
		outer:
		for (ExitRoute r : validDestinations) {
			if (r == null) {
				continue;
			}
			if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
				continue;
			}
			if (used.get(r.destination.getSimpleID())) {
				continue;
			}
			if (!(r.destination.getPipe() instanceof ICraftItems)) {
				continue;
			}
			for (IFilter filter : r.filters) {
				if (filter.blockCrafting()) {
					continue outer;
				}
			}
			ICraftItems crafter = (ICraftItems) r.destination.getPipe();
			outer2:
			for (ItemIdentifierStack craftedItem : crafter.getCraftedItems()) {
				if (!craftableItems.contains(craftedItem.getItem())) {
					for (IFilter filter : r.filters) {
						if (filter.isBlocked() == filter.isFilteredItem(craftedItem.getItem())) {
							continue outer2;
						}
					}
					craftableItems.add(craftedItem.getItem());
				}
			}
			used.set(r.destination.getSimpleID(), true);
		}
		return craftableItems;
	}

	public static int getAmountFor(ItemIdentifier itemType, List<ExitRoute> validDestinations) {
		// TODO: Replace this entire function with a fetch from the pre-built arrays (path incoming later)
		List<Map<ItemIdentifier, Integer>> items = new ArrayList<>(ServerRouter.getBiggestSimpleID());
		for (int i = 0; i < ServerRouter.getBiggestSimpleID(); i++) {
			items.add(new HashMap<>());
		}
		BitSet used = new BitSet(ServerRouter.getBiggestSimpleID());
		outer:
		for (ExitRoute r : validDestinations) {
			if (r == null) {
				continue;
			}
			if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
				continue;
			}
			if (!(r.destination.getPipe() instanceof IProvideItems)) {
				continue;
			}
			for (IFilter filter : r.filters) {
				if (filter.blockProvider()) {
					continue outer;
				}
			}
			IProvideItems provider = (IProvideItems) r.destination.getPipe();
			provider.getAllItems(items.get(r.destination.getSimpleID()), r.filters);
			used.set(r.destination.getSimpleID(), true);
		}
		// TODO: Fix this doubly nested list
		int amount = 0;
		for (Map<ItemIdentifier, Integer> allItems : items) {
			for (Entry<ItemIdentifier, Integer> item : allItems.entrySet()) {
				if (item.getKey().equals(itemType)) {
					amount += item.getValue();
				}
			}
		}
		return amount;
	}
}
