package logisticspipes.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjModel;
import logisticspipes.client.model.mesh.ObjParser;
import logisticspipes.client.model.pipe.PipeModelStore;

/**
 * Parses LP's OBJ models on every resource reload.
 *
 * <p>Replaces the 1.12.2-era loading path, which read the files straight off the classpath
 * with {@code LogisticsPipes.class.getResourceAsStream("/models/...")} during mod init. That
 * had two consequences worth naming: resource packs could not override the geometry, and the
 * files were read before the texture atlases existed, so nothing downstream could resolve a
 * sprite at load time.</p>
 *
 * <p>Parsing happens in the prepare stage, off the render thread; the apply stage only swaps
 * the published map. A file that fails to parse is logged and skipped rather than aborting
 * the reload, which is what the old {@code ClientManager.safeLoadModels} try/catch was
 * standing in for.</p>
 */
public class ObjModelManager implements PreparableReloadListener {

    private static final Logger LOGGER = LogManager.getLogger(ObjModelManager.class);

    /**
     * The models are authored at 100× block scale.
     */
    private static final float MODEL_SCALE = 1 / 100f;

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor,
        PreparationBarrier barrier, Executor applyExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        return CompletableFuture
            .supplyAsync(() -> parseAll(resourceManager), prepareExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::publish, applyExecutor);
    }

    private void publish(Map<Identifier, ObjModel> models) {
        LpObjModels.setLoaded(models);
        // Mod reload listeners apply after the vanilla ones, so ModelEvent.BakingCompleted has
        // already fired by now and anything assembled there saw no models at all. Invalidate
        // so the parts are rebuilt from what we just published.
        PipeModelStore.markDirty();
        // Chunk meshes built before this point contain no pipe geometry; force a rebuild.
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.levelRenderer.allChanged();
    }

    private static Map<Identifier, ObjModel> parseAll(ResourceManager resourceManager) {
        Map<Identifier, ObjModel> models = new HashMap<>();
        for (Identifier location : LpObjModels.ALL) {
            resourceManager.getResource(location).ifPresentOrElse(
                resource -> parse(location, resource, models),
                () -> LOGGER.error("Missing OBJ model {}", location));
        }
        return models;
    }

    private static void parse(Identifier location, Resource resource, Map<Identifier, ObjModel> out) {
        try (InputStream in = resource.open()) {
            ObjModel model = ObjParser.parse(in)
                .mapMeshes(mesh -> mesh.transform(MeshTransforms.scale(MODEL_SCALE)));
            out.put(location, model);
            LOGGER.debug("Loaded OBJ model {} with {} groups", location, model.groups().size());
        } catch (IOException | RuntimeException e) {
            // Keep going: one malformed file must not cost every other model, and a resource
            // pack is free to ship broken geometry.
            LOGGER.error("Failed to parse OBJ model {}", location, e);
        }
    }
}
