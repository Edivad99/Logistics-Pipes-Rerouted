package logisticspipes.client.model.pipe;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logisticspipes.LPConstants;

/**
 * Swaps the pipe's JSON block model for {@link PipeBakedModel} and rebuilds the pipe
 * geometry after every bake.
 *
 * <p>Ordering matters and is why the work is split across two events: the parts are
 * assembled in {@link ModelEvent.BakingCompleted}, by which point the texture atlases exist
 * and {@code TextureRegistrar} has already bound the sprites during the stitch. The 1.12.2
 * path had no equivalent hook — it read the OBJ files at mod init, long before either.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PipeModelRegistration {

    private static final Logger LOGGER = LogManager.getLogger(PipeModelRegistration.class);

    private PipeModelRegistration() {
    }

    private static final ResourceLocation PIPE_BLOCK = LPConstants.rl("pipe");

    @Nullable
    private static PipeBakedModel pipeModel;

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // 1.21.4 replaced ModifyBakingResult#getModels with the whole BakingResult record;
        // the blockstate models are one of its maps, and still mutable in place.
        Map<ModelResourceLocation, BakedModel> models = event.getBakingResult().blockStateModels();

        // Every blockstate variant has to be replaced, not just one. The pipe block declares
        // rotation, model_type and six connection properties, so the baking result holds one
        // entry per permutation — keyed "logisticspipes:pipe#connection_0=false,..." — and
        // there is no entry with an empty variant to look up. They all resolve to the same
        // JSON model, so one PipeBakedModel is shared across them; the geometry comes from
        // ModelData, not from the state.
        List<ModelResourceLocation> variants = models.keySet().stream()
            .filter(id -> id.id().equals(PIPE_BLOCK))
            .filter(id -> !(models.get(id) instanceof PipeBakedModel))
            .toList();
        if (variants.isEmpty()) {
            if (pipeModel == null) {
                LOGGER.error("No baked model found for {}; pipes will not render", PIPE_BLOCK);
            }
            return;
        }

        pipeModel = new PipeBakedModel(models.get(variants.get(0)));
        for (ModelResourceLocation variant : variants) {
            models.put(variant, pipeModel);
        }
        LOGGER.debug("Replaced {} pipe blockstate variants with the baked pipe model", variants.size());
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        // Only invalidate here. The parts cannot be assembled yet: this fires inside the model
        // manager's apply stage, and our OBJ reload listener applies after it. PipeModelStore
        // rebuilds them on first use instead.
        PipeModelStore.markDirty();
        if (pipeModel != null) {
            pipeModel.clearCache();
        }
    }
}
