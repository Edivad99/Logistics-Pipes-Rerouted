package logisticspipes.integrations.ae2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.minecraft.world.level.Level;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import org.jspecify.annotations.Nullable;

/**
 * Runs AE2's own crafting calculation to the end while the caller waits, so a Logistics Pipes
 * request can know right away whether the network can make something and what it lacks.
 *
 * <p>The API only offers {@code ICraftingService.beginCraftingCalculation}, whose calculation runs
 * on a worker thread but only moves while AE2's tick handler lends it time on the server thread at
 * the end of each tick. Waiting on it from the server thread would therefore wait forever. This
 * class lends it that time itself, through the internal {@link CraftingCalculation}, the class the
 * API call wraps.</p>
 *
 * <p>Uses AE2 internals, so callers must be ready for a {@link LinkageError} when AE2 changes them.</p>
 */
final class AECraftingCalculation {

    /**
     * The longest a calculation may hold the server thread; past it, the caller gets no answer.
     */
    private static final long BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    /**
     * How long the calculation runs each time it is lent the thread.
     */
    private static final int SLICE_MICROS = 10_000;

    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "Logistics Pipes AE2 crafting calculation");
        thread.setDaemon(true);
        return thread;
    });

    private AECraftingCalculation() {
    }

    /**
     * Calculates how to craft {@code amount} of {@code what}. Must be called on the server thread.
     *
     * @return the plan, a simulation listing what is missing when it cannot be made in full, or
     *         null when the calculation took too long or broke
     */
    @Nullable
    static ICraftingPlan calculate(Level level, IGrid grid, IActionSource source, AEKey what, long amount,
        CalculationStrategy strategy) {
        // Built here, on the server thread: it takes its snapshot of the network's storage now.
        CraftingCalculation calculation = new CraftingCalculation(level, grid, () -> source,
            new GenericStack(what, amount), strategy);
        CountDownLatch started = new CountDownLatch(1);
        Future<ICraftingPlan> plan = WORKERS.submit(() -> {
            started.countDown();
            return calculation.run();
        });
        try {
            // simulateFor waits for the worker to hand the thread back, so the worker must be running.
            started.await();
            long deadline = System.nanoTime() + BUDGET_NANOS;
            while (!plan.isDone()) {
                if (System.nanoTime() > deadline) {
                    plan.cancel(true);
                    return null;
                }
                calculation.simulateFor(SLICE_MICROS);
            }
            return plan.get();
        } catch (InterruptedException e) {
            plan.cancel(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | RuntimeException e) {
            return null;
        }
    }
}
