package logisticspipes.client.model.pipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

import logisticspipes.client.model.mesh.MeshBaker;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.UvTransform;
import logisticspipes.client.model.tube.TubeKey;
import logisticspipes.client.model.tube.TubeModels;
import logisticspipes.textures.TextureRegistrar;

/**
 * The pipe's block model. Puts pipe geometry into the chunk mesh, where the 1.12.2-derived
 * pipeline had it re-emitted by a block entity renderer on every frame.
 *
 * <p>What this buys, beyond the per-frame cost: the chunk builder applies ambient occlusion
 * and baked block lighting to the quads, neither of which a block entity renderer gets.</p>
 *
 * <p>Geometry depends on the pipe's connections and status rather than on the block state,
 * so it arrives through {@link ModelData} carrying a {@link PipeGeometryKey} that
 * {@code LogisticsGenericPipeBlockEntity.getModelData()} snapshots on the main thread. The key is
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
     * Tube bodies, one entry per tube type and orientation actually placed. Unbounded because
     * there are only a couple of dozen of those.
     */
    private final Map<TubeKey, List<BakedQuad>> tubeCache = new ConcurrentHashMap<>();
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
        tubeCache.clear();
    }

    /**
     * Lets the chunk builder reuse geometry between pipes that resolve to the same
     * configuration: the geometry key is exactly the cache key the quads are built against.
     */
    @Override
    @Nullable
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        ModelData data = level.getModelData(pos);
        PipeGeometryKey key = data.get(PipeModelProperties.GEOMETRY);
        return key != null ? key : data.get(PipeModelProperties.TUBE);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
        List<BlockStateModelPart> parts) {
        ModelData data = level.getModelData(pos);
        PipeGeometryKey key = data.get(PipeModelProperties.GEOMETRY);
        TubeKey tube = data.get(PipeModelProperties.TUBE);
        if ((key == null && tube == null) || !PipeModelStore.isReady()) {
            return;
        }

        int generation = PipeModelStore.generation();
        if (cachedGeneration.getAndSet(generation) != generation) {
            quadCache.invalidateAll();
            tubeCache.clear();
        }

        if (tube != null) {
            List<BakedQuad> quads = tubeCache.computeIfAbsent(tube, PipeBakedModel::bakeTube);
            if (!quads.isEmpty()) {
                parts.add(new Part(quads, particleMaterial()));
            }
            return;
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
     * A tube body on its block-atlas sprite. Baked double-sided because the chunk mesh culls back
     * faces, where the block entity renderer drew the single-sided mesh with culling off; the two
     * windings never show at once, so they do not fight for depth the way two copies did there.
     * Not shaded, matching the directional-light-free render type the tubes had.
     */
    private static List<BakedQuad> bakeTube(TubeKey key) {
        ObjMesh mesh = TubeModels.mesh(key.kind(), key.orientation());
        Identifier spriteName = key.kind().sprite;
        if (mesh.isEmpty() || spriteName == null) {
            return List.of();
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getAtlasManager()
            .getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(spriteName);
        List<BakedQuad> quads = new ArrayList<>();
        MeshBaker.bake(quads, mesh.doubleSided(), sprite, UvTransform.IDENTITY, MeshBaker.WHITE, false);
        return List.copyOf(quads);
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
     * {@link PipeModelProperties#PARTICLE_SPRITE}. A normal pipe uses its per-type face
     * texture, the one on its unconnected sides, so each pipe kind breaks in its own colour,
     * unless that texture is blank and the frame is all there is to see.
     */
    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        ModelData data = level.getModelData(pos);
        Identifier name = data.get(PipeModelProperties.PARTICLE_SPRITE);
        if (name != null) {
            return new Material.Baked(Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(name), false);
        }
        TubeKey tube = data.get(PipeModelProperties.TUBE);
        if (tube != null && tube.kind().sprite != null) {
            return new Material.Baked(Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(tube.kind().sprite), false);
        }
        PipeGeometryKey key = data.get(PipeModelProperties.GEOMETRY);
        if (key != null) {
            TextureAtlasSprite icon = TextureRegistrar.pipeParticleSprite(key.textureIndex());
            if (icon != null) {
                return new Material.Baked(icon, false);
            }
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
