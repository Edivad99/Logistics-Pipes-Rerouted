package logisticspipes.client.model.pipe;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * The pipe's block model. Puts pipe geometry into the chunk mesh, where the 1.12.2-derived
 * pipeline had it re-emitted by a block entity renderer on every frame.
 *
 * <p>What this buys, beyond the per-frame cost: the chunk builder applies ambient occlusion
 * and baked block lighting to the quads, neither of which a block entity renderer gets.</p>
 *
 * <p>Geometry depends on the pipe's connections and status rather than on the block state,
 * so it arrives through {@link ModelData} carrying a {@link PipeGeometryKey} that
 * {@code LogisticsTileGenericPipe.getModelData()} snapshots on the main thread. The key is
 * also the cache key: a whole base typically resolves to a handful of distinct
 * configurations, so the quads are built once and shared.</p>
 */
@OnlyIn(Dist.CLIENT)
public class PipeBakedModel implements IDynamicBakedModel {

    /**
     * Bounded because the key space is large in principle — the neighbour bounds are
     * continuous — even though real worlds only hit a few dozen entries.
     */
    private static final int CACHE_SIZE = 512;

    private final BakedModel fallback;
    private final Cache<PipeGeometryKey, List<BakedQuad>> quadCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .build();
    /**
     * The {@link PipeModelStore} generation the cache was filled against. Because the parts
     * are now assembled lazily, they can change after quads have already been cached.
     */
    private final AtomicInteger cachedGeneration = new AtomicInteger(-1);

    /**
     * @param fallback the JSON model this replaces, kept for the particle sprite and the item
     *                 transforms so held and dropped pipes still behave
     */
    public PipeBakedModel(BakedModel fallback) {
        this.fallback = fallback;
    }

    /**
     * Drops cached geometry; called when models or resources reload.
     */
    public void clearCache() {
        quadCache.invalidateAll();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
        RandomSource random, ModelData modelData, @Nullable RenderType renderType) {
        // Side-specific queries are for face culling against neighbours; pipe geometry is not
        // flush with the block faces, so all of it lives in the null-side bucket.
        if (side != null) {
            return List.of();
        }

        PipeGeometryKey key = modelData.get(PipeModelProperties.GEOMETRY);
        if (key == null || !PipeModelStore.isReady()) {
            return List.of();
        }

        int generation = PipeModelStore.generation();
        if (cachedGeneration.getAndSet(generation) != generation) {
            quadCache.invalidateAll();
        }

        List<BakedQuad> cached = quadCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        List<BakedQuad> baked = PipeQuadBaker.bake(PipeModelStore.parts(), PipeModelStore.sprites(), key);
        quadCache.put(key, baked);
        return baked;
    }

    @Override
    public boolean useAmbientOcclusion() {
        // This selects which of two lighting paths the chunk builder uses, and neither is a
        // good fit for geometry suspended inside the block rather than flush with its faces:
        //
        //  - false takes ModelBlockRenderer.tesselateWithoutAO, which for the side == null
        //    bucket recomputes light per quad from pos.relative(quad.getDirection()). A quad
        //    facing down samples the block underneath — the ground — and comes out dark even
        //    with air all around the pipe.
        //  - true takes tesselateWithAO, which blends light per vertex from the surrounding
        //    blocks. Smoother, but still derived from neighbours the pipe does not touch.
        //
        // The immediate-mode renderer sidestepped the question entirely by passing one
        // packedLight for the whole pipe, taken at its own position. Off, because AO visibly
        // banded the joints; the per-quad neighbour sampling that leaves is steered back onto
        // the pipe's own position by MeshBaker.lightSampleFace, which reproduces the single
        // uniform light level of the immediate-mode path.
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        TextureAtlasSprite sprite = PipeModelStore.sprites().basicPipe();
        return sprite != null ? sprite : fallback.getParticleIcon();
    }

    /**
     * Break and hit particles go through this overload, which is where a pipe that does not
     * draw the frame — the request table — gets to name its own sprite through
     * {@link PipeModelProperties#PARTICLE_SPRITE}.
     */
    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        ResourceLocation name = modelData.get(PipeModelProperties.PARTICLE_SPRITE);
        if (name != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(name);
            if (sprite != null) {
                return sprite;
            }
        }
        return getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return fallback.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
