package logisticspipes.request;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.Translations;
import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.to_client.orderer.OrdererContentMessage;
import logisticspipes.network.to_client.orderer.RequestAnswerMessage;
import logisticspipes.network.to_client.orderer.RequestComponentsMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestTree.ActiveRequestType;
import logisticspipes.request.order.OrderTree;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class RequestHandler {

	/** Answers reach a player, not a position: the request may have come from another dimension. */
	private static void sendToPlayer(Player player, CustomPacketPayload payload) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, payload);
		}
	}

	public enum DisplayOptions {
		Both,
		SupplyOnly,
		CraftOnly
	}

	public static void request(final Player player, final ItemIdentifierStack stack, final CoreRoutedPipe pipe) {
		if (!pipe.useEnergy(5)) {
			player.sendOverlayMessage(Component.translatable(Translations.Chat.NO_ENERGY));
			return;
		}
		RequestTree.request(new ItemIdentifierStack(stack), pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), true));
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {
				sendToPlayer(player, new RequestAnswerMessage(List.of(item), false));
				if (pipe instanceof IRequestWatcher) {
					((IRequestWatcher) pipe).handleOrderList(item, parts);
				}
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {}
		}, null);
	}

	public static void simulate(final Player player, final ItemIdentifierStack stack, CoreRoutedPipe pipe) {
		final List<IResource> usedList = new ArrayList<>();
		final List<IResource> missingList = new ArrayList<>();
		RequestTree.simulate(new ItemIdentifierStack(stack), pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				missingList.addAll(resources);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {
				usedList.addAll(resources);
			}
		});
		sendToPlayer(player, new RequestComponentsMessage(List.copyOf(usedList), List.copyOf(missingList)));
	}

	public static void refresh(Player player, CoreRoutedPipe pipe, DisplayOptions option) {
		Map<ItemIdentifier, Integer> availableItems;
		LinkedList<ItemIdentifier> craftableItems;

		if (option == DisplayOptions.SupplyOnly || option == DisplayOptions.Both) {
			availableItems = NetworkItems.getAvailableItems(pipe.getRouter().getIRoutersByCost());
		} else {
			availableItems = new HashMap<>();
		}
		if (option == DisplayOptions.CraftOnly || option == DisplayOptions.Both) {
			craftableItems = NetworkItems.getCraftableItems(pipe.getRouter().getIRoutersByCost());
		} else {
			craftableItems = new LinkedList<>();
		}
		TreeSet<ItemIdentifierStack> allItems = new TreeSet<>();

		for (Entry<ItemIdentifier, Integer> item : availableItems.entrySet()) {
			ItemIdentifierStack newStack = item.getKey().makeStack(item.getValue());
			allItems.add(newStack);
		}

		for (ItemIdentifier item : craftableItems) {
			if (availableItems.containsKey(item)) {
				continue;
			}
			allItems.add(item.makeStack(0));
		}
		sendToPlayer(player, new OrdererContentMessage(List.copyOf(allItems)));
	}

	public static void requestList(final Player player, final List<ItemIdentifierStack> list, final CoreRoutedPipe pipe) {
		if (!pipe.useEnergy(5)) {
			player.sendOverlayMessage(Component.translatable(Translations.Chat.NO_ENERGY));
			return;
		}
		RequestTree.request(list, pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), true));
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), false));
				if (pipe instanceof IRequestWatcher) {
					((IRequestWatcher) pipe).handleOrderList(null, parts);
				}
			}
		}, RequestTree.defaultRequestFlags, null);
	}

	public static void requestMacrolist(CompoundTag itemlist, final CoreRoutedPipe requester, final Player player) {
		if (!requester.useEnergy(5)) {
			player.sendOverlayMessage(Component.translatable(Translations.Chat.NO_ENERGY));
			return;
		}
		ListTag list = itemlist.getListOrEmpty("inventar");
		final List<ItemIdentifierStack> transaction = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			ItemIdentifierStack stack = ItemIdentifierStack.loadFromNBT(list.getCompoundOrEmpty(i), player.registryAccess());
			if (stack != null) {
				transaction.add(stack);
			}
		}
		RequestTree.request(transaction, requester, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), true));
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), false));
				if (requester instanceof IRequestWatcher) {
					((IRequestWatcher) requester).handleOrderList(null, parts);
				}
			}
		}, RequestTree.defaultRequestFlags, null);
	}

	public static Object[] computerRequest(final ItemIdentifierStack makeStack, final CoreRoutedPipe pipe, boolean craftingOnly) {

		EnumSet<ActiveRequestType> requestFlags;
		if (craftingOnly) {
			requestFlags = EnumSet.of(ActiveRequestType.Craft);
		} else {
			requestFlags = EnumSet.of(ActiveRequestType.Craft, ActiveRequestType.Provide);
		}
		if (!pipe.useEnergy(15)) {
			return new Object[] { "NO_POWER" };
		}
		final Object[] status = new Object[2];
		RequestTree.request(makeStack, pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				status[0] = "MISSING";
				status[1] = resources;
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {
				status[0] = "DONE";
				List<IResource> itemList = new LinkedList<>();
				itemList.add(item);
				status[1] = itemList;
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {}
		}, false, false, true, false, requestFlags, null);
		return status;
	}

	public static void refreshFluid(Player player, CoreRoutedPipe pipe) {
		TreeSet<FluidIdentifierStack> allItems = getAvailableFluid(pipe.getRouter().getIRoutersByCost());
		sendToPlayer(player, new OrdererContentMessage(allItems.stream()
				.map(item -> new ItemIdentifierStack(item.getFluid().getItemIdentifier(), item.getAmount()))
				.toList()));
	}

	/** Every fluid the reachable providers offer, amounts summed across them and capped at Integer.MAX_VALUE. */
	private static TreeSet<FluidIdentifierStack> getAvailableFluid(List<ExitRoute> validDestinations) {
		Map<FluidIdentifier, Integer> allAvailableItems = new HashMap<>();
		for (ExitRoute r : validDestinations) {
			if (r == null) {
				continue;
			}
			if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
				continue;
			}
			if (!(r.destination.getPipe() instanceof IProvideFluids provider)) {
				continue;
			}

			Map<FluidIdentifier, Integer> allItems = provider.getAvailableFluids();

			for (Entry<FluidIdentifier, Integer> liquid : allItems.entrySet()) {
				Integer amount = allAvailableItems.get(liquid.getKey());
				if (amount == null) {
					allAvailableItems.put(liquid.getKey(), liquid.getValue());
				} else {
					long addition = ((long) amount) + liquid.getValue();
					if (addition > Integer.MAX_VALUE) {
						addition = Integer.MAX_VALUE;
					}
					allAvailableItems.put(liquid.getKey(), (int) addition);
				}
			}
		}
		return allAvailableItems.entrySet().stream()
				.map(item -> new FluidIdentifierStack(item.getKey(), item.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public static void requestFluid(final Player player, final ItemIdentifierStack stack, CoreRoutedPipe pipe, IRequestFluid requester) {
		if (!pipe.useEnergy(10)) {
			player.sendOverlayMessage(Component.translatable(Translations.Chat.NO_ENERGY));
			return;
		}

		RequestTree.requestFluid(FluidIdentifier.get(stack.getItem()), stack.getStackSize(), requester, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				sendToPlayer(player, new RequestAnswerMessage(List.copyOf(resources), true));
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, @Nullable OrderTree parts) {
				sendToPlayer(player, new RequestAnswerMessage(List.of(item), false));
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, OrderTree parts) {}
		});
	}
}
