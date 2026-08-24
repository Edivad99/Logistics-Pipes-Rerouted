package logisticspipes.client.model.pipe;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jspecify.annotations.Nullable;

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
 *
 * <p>1.21.5 removed {@code BakedModel} and split it per use site; a block model is now a
 * {@link BlockStateModel} handing out {@link BlockStateModelPart}s. The NeoForge-added
 * {@code collectParts} overload is what replaces {@code IDynamicBakedModel#getQuads}: instead
 * of the model data being pushed in as an argument, the model pulls it off the level with
 * {@code level.getModelData(pos)}.</p>
 */
public class PipeBakedModel implements DynamicBlockStateModel {

    /**
     * Bounded because the key space is large in principle — the neighbour bounds are
     * continuous — even though real worlds only hit a few dozen entries.
     */
    private static final int CACHE_SIZE = 512;

    private final BlockStateModel fallback;
    private final Cache<PipeGeometryKey, List<BakedQuad>> quadCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .build();
    /**
     * The {@link PipeModelStore} generation the cache was filled against. Because the parts
     * are now assembled lazily, they can change after quads have already been cached.
     */
    private final AtomicInteger cachedGeneration = new AtomicInteger(-1);

    /**
     * @param fallback the JSON model this replaces, kept for the particle sprite so pipes
     *                 whose own sprites have not been stitched yet still break visibly
     */
    public PipeBakedModel(BlockStateModel fallback) {
        this.fallback = fallback;
    }

    /**
     * Drops cached geometry; called when models or resources reload.
     */
    public void clearCache() {
        quadCache.invalidateAll();
    }

    /**
     * Lets the chunk builder reuse geometry between pipes that resolve to the same
     * configuration: the geometry key is exactly the cache key the quads are built against.
     */
    @Override
    @Nullable
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return level.getModelData(pos).get(PipeModelProperties.GEOMETRY);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
        List<BlockStateModelPart> parts) {
        PipeGeometryKey key = level.getModelData(pos).get(PipeModelProperties.GEOMETRY);
        if (key == null || !PipeModelStore.isReady()) {
            return;
        }

        int generation = PipeModelStore.generation();
        if (cachedGeneration.getAndSet(generation) != generation) {
            quadCache.invalidateAll();
        }

        List<BakedQuad> quads = quadCache.getIfPresent(key);
        if (quads == null) {
            quads = PipeQuadBaker.bake(PipeModelStore.parts(), PipeModelStore.sprites(), key);
            quadCache.put(key, quads);
        }
        if (!quads.isEmpty()) {
            parts.add(new Part(quads, particleMaterial()));
        }
    }

    /**
     * No material flags. 26.1.2 uses them to tell the chunk builder about things like forced
     * translucency or animation; pipe geometry is plain cutout.
     */
    @Override
    public int materialFlags() {
        return 0;
    }

    @Override
    public Material.Baked particleMaterial() {
        TextureAtlasSprite sprite = PipeModelStore.sprites().basicPipe();
        return sprite != null ? new Material.Baked(sprite, false) : fallback.particleMaterial();
    }

    /**
     * Break and hit particles go through this overload, which is where a pipe that does not
     * draw the frame — the request table — gets to name its own sprite through
     * {@link PipeModelProperties#PARTICLE_SPRITE}.
     */
    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Identifier name = level.getModelData(pos).get(PipeModelProperties.PARTICLE_SPRITE);
        if (name != null) {
            return new Material.Baked(Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(name), false);
        }
        return particleMaterial();
    }

    /**
     * The single part every pipe renders as.
     *
     * <p>{@code useAmbientOcclusion} being false selects which of two lighting paths the
     * chunk builder uses, and neither is a good fit for geometry suspended inside the block
     * rather than flush with its faces:
     *
     * <ul>
     *   <li>false takes {@code ModelBlockRenderer.tesselateWithoutAO}, which for the
     *   {@code side == null} bucket recomputes light per quad from
     *   {@code pos.relative(quad.getDirection())}. A quad facing down samples the block
     *   underneath — the ground — and comes out dark even with air all around the pipe.</li>
     *   <li>true takes {@code tesselateWithAO}, which blends light per vertex from the
     *   surrounding blocks. Smoother, but still derived from neighbours the pipe does not
     *   touch.</li>
     * </ul>
     *
     * <p>The immediate-mode renderer sidestepped the question entirely by passing one
     * packedLight for the whole pipe, taken at its own position. Off, because AO visibly
     * banded the joints; the per-quad neighbour sampling that leaves is steered back onto the
     * pipe's own position by {@code MeshBaker.lightSampleFace}, which reproduces the single
     * uniform light level of the immediate-mode path.</p>
     */
    private record Part(List<BakedQuad> quads, Material.Baked particleMaterial) implements BlockStateModelPart {

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            // Side-specific queries are for face culling against neighbours; pipe geometry is
            // not flush with the block faces, so all of it lives in the null-side bucket.
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}
