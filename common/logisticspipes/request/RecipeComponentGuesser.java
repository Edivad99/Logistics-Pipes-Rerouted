package logisticspipes.request;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * Picks, for each slot of an imported recipe, which of its interchangeable ingredients the
 * network is best able to supply.
 */
public final class RecipeComponentGuesser {

    /**
     * How much of an ingredient has to be available before the network stops looking for a
     * craftable alternative.
     */
    private static final int ENOUGH = 64;

    private RecipeComponentGuesser() {}

    /**
     * For every slot, the index into its candidate list that the player should use, or empty when
     * the network can neither supply nor craft any of them.
     */
    public static List<Optional<Integer>> choose(CoreRoutedPipe pipe, List<List<ItemIdentifier>> candidates) {
        // Only worth asking the network what it can craft if something is actually short.
        final Supplier<List<ItemIdentifier>> craftable = Suppliers.memoize(() ->
                SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost()));
        return candidates.stream().map(slot -> choose(pipe, slot, craftable)).toList();
    }

    private static Optional<Integer> choose(CoreRoutedPipe pipe, List<ItemIdentifier> candidates,
            Supplier<List<ItemIdentifier>> craftable) {
        int best = -1;
        int bestAmount = 0;
        for (int i = 0; i < candidates.size(); i++) {
            final int amount = SimpleServiceLocator.logisticsManager
                    .getAmountFor(candidates.get(i), pipe.getRouter().getIRoutersByCost());
            if (amount > bestAmount) {
                bestAmount = amount;
                best = i;
            }
        }
        if (bestAmount < ENOUGH) {
            // Little of anything in stock, so prefer whatever the network can make more of.
            for (int i = 0; i < candidates.size(); i++) {
                if (craftable.get().contains(candidates.get(i))) {
                    return Optional.of(i);
                }
            }
        }
        return best == -1 ? Optional.empty() : Optional.of(best);
    }
}
