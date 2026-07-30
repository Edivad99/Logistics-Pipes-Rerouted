package logisticspipes.client.model.pipe;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logisticspipes.client.model.LpObjModels;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.client.model.tube.TubeModels;

/**
 * Holds the current {@link PipeModelParts} and {@link PipeSprites}, republished whenever
 * resources reload.
 *
 * <p>The two halves arrive from unrelated events, and their order is not guaranteed: sprites
 * are bound during the atlas stitch (inside the model manager's apply stage), while the OBJ
 * geometry is published by our own reload listener — and mod reload listeners apply
 * <em>after</em> the vanilla ones. Building the parts eagerly from
 * {@code ModelEvent.BakingCompleted} therefore ran before the OBJ files existed and produced
 * an empty set.</p>
 *
 * <p>So the parts are rebuilt lazily instead: any event that could invalidate them just calls
 * {@link #markDirty()}, and the first reader afterwards does the work. {@link #generation()}
 * lets {@link PipeBakedModel} notice its cached quads are stale.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PipeModelStore {

    private static final Logger LOGGER = LogManager.getLogger(PipeModelStore.class);

    private PipeModelStore() {
    }

    private static volatile PipeModelParts parts = PipeModelParts.empty();
    private static volatile PipeSprites sprites = PipeSprites.empty();
    /**
     * Assembled alongside the pipe parts — same OBJ reload, same invalidation.
     */
    private static volatile SolidBlockModelParts solidBlock = SolidBlockModelParts.empty();
    private static volatile int generation;
    private static volatile boolean dirty = true;
    /**
     * Guards against spamming the log every frame while something is genuinely missing.
     */
    private static volatile boolean reportedNotReady;

    /**
     * Marks the assembled parts stale. Safe to call from any thread and from either of the
     * two events that can invalidate them.
     */
    public static void markDirty() {
        dirty = true;
        reportedNotReady = false;
    }

    public static PipeModelParts parts() {
        if (dirty) {
            rebuild();
        }
        return parts;
    }

    public static PipeSprites sprites() {
        return sprites;
    }

    public static SolidBlockModelParts solidBlock() {
        if (dirty) {
            rebuild();
        }
        return solidBlock;
    }

    public static void setSprites(PipeSprites newSprites) {
        sprites = newSprites;
        reportedNotReady = false;
    }

    /**
     * Bumped on every successful rebuild, so cached geometry can be detected as stale.
     */
    public static int generation() {
        return generation;
    }

    private static synchronized void rebuild() {
        if (!dirty) {
            return;
        }
        if (!LpObjModels.isLoaded()) {
            // The reload listener has not published yet; stay dirty and try again later
            // rather than caching an empty result.
            return;
        }
        dirty = false;

        List<String> problems = new ArrayList<>();
        PipeModelParts loaded = PipeModelPartsLoader.load(problems::add);
        if (!problems.isEmpty()) {
            LOGGER.error("Pipe model assembly reported {} problem(s):", problems.size());
            problems.forEach(problem -> LOGGER.error("  {}", problem));
        }
        parts = loaded;
        solidBlock = SolidBlockModelParts.load();
        TubeModels.reload();
        generation++;
    }

    /**
     * True once both halves are usable. Logs the reason the first time it is not, so a blank
     * pipe is diagnosable from the log instead of by bisecting the render path.
     */
    public static boolean isReady() {
        PipeModelParts currentParts = parts();
        PipeSprites currentSprites = sprites;
        if (!currentParts.isEmpty() && currentSprites.isComplete()) {
            return true;
        }

        if (!reportedNotReady) {
            reportedNotReady = true;
            LOGGER.error("Pipe geometry unavailable: objModelsLoaded={} parts={} sprites={}",
                LpObjModels.isLoaded(),
                currentParts.isEmpty() ? "empty" : "ok",
                currentSprites.isComplete() ? "ok" : describeMissingSprites(currentSprites));
        }
        return false;
    }

    private static String describeMissingSprites(PipeSprites current) {
        List<String> missing = new ArrayList<>();
        if (current.basicPipe() == null) {
            missing.add("basicPipe");
        }
        if (current.status() == null) {
            missing.add("status");
        }
        if (current.statusBC() == null) {
            missing.add("statusBC");
        }
        return "missing " + missing;
    }
}
