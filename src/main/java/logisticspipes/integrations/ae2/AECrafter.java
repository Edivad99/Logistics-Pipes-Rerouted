package logisticspipes.integrations.ae2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.world.level.Level;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.inventory.IExternalCrafter;
import logisticspipes.inventory.IExternalCrafter.SettledJob;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * Hands requests to the autocrafting of an Applied Energistics 2 network, found through an ME
 * Interface.
 *
 * <p>Calculations are run to the end on the spot by {@link AECraftingCalculation}, so a request
 * knows at once whether the network can make something, and what it lacks, exactly as a terminal
 * would say. Should AE2's internals have changed, it falls back on the API, whose calculation only
 * answers a few ticks later: requests then assume the network can make anything it has a pattern
 * for, and the job turns the order down afterwards when it cannot.</p>
 *
 * <p>Jobs are submitted the way a player's request from a terminal is, with no requesting machine:
 * the network puts the result in its storage, where the provider facing the interface picks it up.
 * That spares Logistics Pipes from joining the grid, at the price of not hearing when a job is
 * cancelled from a terminal. AE2 saves the job with the world, so a request survives a reload as
 * long as the provider remembers the order it belongs to.</p>
 */
public final class AECrafter implements IExternalCrafter {

    /**
     * Whether {@link AECraftingCalculation} still works with this version of AE2; cleared the first
     * time it does not link.
     */
    private static volatile boolean internalsUsable = true;

    private final IGrid grid;
    private final ICraftingService crafting;
    private final Level level;
    private final IActionSource source;

    public AECrafter(IGrid grid, Level level, IActionSource source) {
        this.grid = grid;
        this.crafting = grid.getCraftingService();
        this.level = level;
        this.source = source;
    }

    @Override
    public Set<ItemIdentifier> getCraftables() {
        Set<ItemIdentifier> result = new HashSet<>();
        for (AEKey key : crafting.getCraftables(AEItemKey.filter())) {
            if (key instanceof AEItemKey itemKey) {
                result.add(ItemIdentifier.get(itemKey.getReadOnlyStack()));
            }
        }
        return result;
    }

    @Override
    public boolean canCraft(ItemIdentifier item) {
        AEItemKey key = AEItemKey.of(item.makeNormalStack(1));
        return key != null && crafting.isCraftable(key);
    }

    @Override
    public Calculation calculate(ItemIdentifier item, int amount) {
        AEItemKey key = AEItemKey.of(item.makeNormalStack(1));
        if (key == null || amount <= 0) {
            return new Calculation(0, Map.of());
        }
        // Crafting less finds the most that can be made; when that is nothing, its plan is the
        // simulation of the whole amount, with what is missing.
        ICraftingPlan plan = calculateNow(key, amount, CalculationStrategy.CRAFT_LESS);
        if (plan == null) {
            return Calculation.assumeCraftable(amount);
        }
        if (plan.simulation()) {
            return new Calculation(0, missingItems(plan));
        }
        int craftable = (int) Math.min(plan.finalOutput().amount(), amount);
        if (craftable >= amount) {
            return new Calculation(amount, Map.of());
        }
        // Only part of it: ask again for the whole amount to learn what the rest lacks.
        ICraftingPlan whole = calculateNow(key, amount, CalculationStrategy.REPORT_MISSING_ITEMS);
        return new Calculation(craftable, whole == null ? Map.of() : missingItems(whole));
    }

    @Override
    public Job startCrafting(ItemIdentifier item, int amount) {
        AEItemKey key = AEItemKey.of(item.makeNormalStack(1));
        if (key == null || amount <= 0) {
            return SettledJob.FAILED;
        }
        ICraftingPlan plan = calculateNow(key, amount, CalculationStrategy.REPORT_MISSING_ITEMS);
        if (plan != null) {
            return submit(plan) ? SettledJob.ACCEPTED : SettledJob.FAILED;
        }
        Future<ICraftingPlan> later = crafting.beginCraftingCalculation(level, () -> source, key, amount,
            CalculationStrategy.REPORT_MISSING_ITEMS);
        return new PendingJob(later);
    }

    /**
     * Runs the calculation to the end now, or returns null when that cannot be done.
     */
    @Nullable
    private ICraftingPlan calculateNow(AEKey key, long amount, CalculationStrategy strategy) {
        if (!internalsUsable) {
            return null;
        }
        try {
            return AECraftingCalculation.calculate(level, grid, source, key, amount, strategy);
        } catch (LinkageError e) {
            internalsUsable = false;
            LogisticsPipes.LOG.warn("This version of Applied Energistics 2 changed the crafting "
                + "calculation; requests will no longer know beforehand what it cannot craft", e);
            return null;
        }
    }

    private boolean submit(ICraftingPlan plan) {
        // A simulation is what AE2 hands back when something is missing: it cannot be started.
        if (plan.simulation()) {
            return false;
        }
        ICraftingSubmitResult submitted = crafting.submitJob(plan, null, null, false, source);
        return submitted.successful();
    }

    /**
     * The items a simulated plan lacks. Other kinds, such as fluids, cannot be named in a request.
     */
    private static Map<ItemIdentifier, Integer> missingItems(ICraftingPlan plan) {
        Map<ItemIdentifier, Integer> missing = new HashMap<>();
        for (Object2LongMap.Entry<AEKey> entry : plan.missingItems()) {
            if (entry.getKey() instanceof AEItemKey itemKey) {
                int count = (int) Math.min(entry.getLongValue(), Integer.MAX_VALUE);
                missing.merge(ItemIdentifier.get(itemKey.getReadOnlyStack()), count, Integer::sum);
            }
        }
        return missing;
    }

    /**
     * A job whose calculation is left to AE2's tick handler, when it could not be run on the spot.
     */
    private final class PendingJob implements Job {

        private final Future<ICraftingPlan> plan;
        private State state = State.PENDING;

        private PendingJob(Future<ICraftingPlan> plan) {
            this.plan = plan;
        }

        @Override
        public State poll() {
            if (state != State.PENDING || !plan.isDone()) {
                return state;
            }
            ICraftingPlan result = getPlan();
            state = result != null && submit(result) ? State.ACCEPTED : State.FAILED;
            return state;
        }

        @Nullable
        private ICraftingPlan getPlan() {
            try {
                return plan.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException | RuntimeException e) {
                return null;
            }
        }

        @Override
        public State state() {
            return state;
        }

        @Override
        public void cancel() {
            if (state == State.PENDING) {
                plan.cancel(true);
                state = State.FAILED;
            }
        }
    }
}
