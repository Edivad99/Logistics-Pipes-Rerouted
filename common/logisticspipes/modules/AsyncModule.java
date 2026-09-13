package logisticspipes.modules;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.connection.NeighborBlockEntity;
import network.rs485.grow.LPExecutors;

/**
 * A module whose work is planned off the server thread and applied on it.
 *
 * <p>Every tick the module either lets a running job continue, applies a finished one, or -- once
 * every {@link #getEveryNthTick()} ticks -- gathers a setup object on the server thread and starts
 * a new job with it.
 *
 * @param <S> what {@link #jobSetup()} gathers on the server thread and hands to the background job
 * @param <C> what the background job produces for {@link #completeJob}
 */
public abstract class AsyncModule<S extends @Nullable Object, C extends @Nullable Object>
    extends LogisticsModule {

    /** A job that outruns this is abandoned rather than left to pile up. */
    private static final long JOB_TIMEOUT_SECONDS = 90;

    private @Nullable CompletableFuture<@Nullable C> currentJob;

    /**
     * The wait time in ticks until the next job is started.
     */
    public int getEveryNthTick() {
        return 20;
    }

    /**
     * A debug helper for adding the connected {@link BlockEntity} to error information. May return
     * null, if the information is not available.
     */
    private @Nullable BlockEntity getConnectedEntity() {
        if (service == null) {
            return null;
        }
        List<NeighborBlockEntity<BlockEntity>> inventories = service.getAvailableAdjacent().inventories();
        return inventories.isEmpty() ? null : inventories.getFirst().getBlockEntity();
    }

    @Override
    public void tick() {
        CompletableFuture<@Nullable C> job = currentJob;
        if (job != null && !job.isDone()) {
            runSyncWork();
            return;
        }
        if (job != null) {
            try {
                runSyncWork();
                completeJob(job.getNow(null));
            } finally {
                currentJob = null;
            }
        }
        if (service != null && service.isNthTick(getEveryNthTick())) {
            S setup = jobSetup();
            currentJob = CompletableFuture.supplyAsync(() -> tickAsync(setup), LPExecutors.async())
                .orTimeout(JOB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                // Always completes normally, so tick() can read the value without guarding.
                .exceptionally(throwable -> {
                    try {
                        reportJobFailure(throwable);
                    } catch (RuntimeException reportingFailure) {
                        // The pipe may be gone by now, so gathering the details can fail on its
                        // own. Never let that reach the tick, which would take the server down.
                        LogisticsPipes.LOG.error("Error in ticking async module", throwable);
                    }
                    return null;
                });
        }
    }

    private void reportJobFailure(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
            ? throwable.getCause() : throwable;
        // A cancelled job lost its server; a timed out one is almost always a paused game.
        if (cause instanceof TimeoutException || cause instanceof CancellationException || isGamePaused()) {
            return;
        }
        BlockEntity connectedEntity = getConnectedEntity();
        String connected = connectedEntity == null ? ""
            : " connected to " + connectedEntity + " at " + connectedEntity.getBlockPos();
        LogisticsPipes.LOG.error("Error in ticking async module {}{}", getLPName(), connected, cause);
    }

    /** A paused single player game stops the server thread, so its jobs time out through no fault. */
    private boolean isGamePaused() {
        Level world = getWorld();
        if (world == null || world.isClientSide() || FMLEnvironment.getDist() != Dist.CLIENT) {
            return false;
        }
        return Minecraft.getInstance().isPaused();
    }

    /**
     * Setup function which is run on every new module tick, on the server thread.
     *
     * @return setup object S which is passed to {@link #tickAsync}.
     */
    public abstract S jobSetup();

    /**
     * Completion function that is run on the server thread after every module tick whose
     * asynchronous work is done.
     *
     * @param result what {@link #tickAsync} returned, or null if it failed or timed out.
     */
    public abstract void completeJob(@Nullable C result);

    /**
     * The work that runs off the server thread, under a timeout. Takes the setup object and returns
     * an object for {@link #completeJob}.
     */
    public abstract C tickAsync(S setupObject);

    /**
     * Runs every tick while the current job is still going.
     */
    public abstract void runSyncWork();
}
