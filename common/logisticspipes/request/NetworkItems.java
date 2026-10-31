package logisticspipes.request;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What the providers and crafters a pipe can reach offer, for the request GUIs, signs and integrations.
 */
public final class NetworkItems {

    private NetworkItems() {
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
        outer:
        for (ExitRoute r : validDestinations) {
            if (r == null) {
                continue;
            }
            if (!r.containsFlag(PipeRoutingConnectionType.CAN_REQUEST_FROM)) {
                continue;
            }
            if (!(r.destination.getPipe() instanceof IProvideItems provider)) {
                continue;
            }
            for (IFilter filter : r.filters) {
                if (filter.blockProvider()) {
                    continue outer;
                }
            }
            provider.getAllItems(items.get(r.destination.getSimpleID()), r.filters);
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
            if (!(r.destination.getPipe() instanceof ICraftItems crafter)) {
                continue;
            }
            for (IFilter filter : r.filters) {
                if (filter.blockCrafting()) {
                    continue outer;
                }
            }
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

    /**
     * How many of an item the reachable providers offer in total.
     */
    public static int getAmountFor(ItemIdentifier itemType, List<ExitRoute> validDestinations) {
        return getAvailableItems(validDestinations).getOrDefault(itemType, 0);
    }
}
