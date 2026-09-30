package logisticspipes.client.renderer.blockentity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.level.block.LogisticsSolidBlock;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

/**
 * Shared BlockEntityRenderer for all LP solid blocks. Reuses the OBJ-parsed 3D body
 * and 5 cover plates held by {@link SolidBlockModelParts} and renders them to the
 * cutoutMipped buffer via {@link MeshRenderer}.
 *
 * <p>Each {@link LogisticsSolidBlock} names the sprite it wants, found at
 * {@code logisticspipes:solid_block/<name>} which is used as the plate texture.</p>
 */
public class LogisticsSolidBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, SolidBlockRenderState> {

    private static final Map<String, TextureAtlasSprite> SPRITE_CACHE = new HashMap<>();
    private static final Map<String, TextureAtlasSprite> SPRITE_CACHE_ACTIVE = new HashMap<>();

    public LogisticsSolidBlockRenderer(BlockEntityRendererProvider.Context context) {
    }


    public static TextureAtlasSprite getIcon(LogisticsSolidBlock block) {
        return getIcon(block, false);
    }

    /**
     * LP1: types with an active texture switch to {@code <name>_active} while the tile is active.
     */
    public static TextureAtlasSprite getIcon(LogisticsSolidBlock block, boolean active) {
        boolean useActive = active && block.hasActiveTexture();
        Map<String, TextureAtlasSprite> cache = useActive ? SPRITE_CACHE_ACTIVE : SPRITE_CACHE;
        String name = block.textureName() + (useActive ? "_active" : "");
        TextureAtlasSprite cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getAtlasManager()
            .getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(LPConstants.rl("solid_block/" + name));
        cache.put(name, sprite);
        return sprite;
    }

    public static void clearCache() {
        SPRITE_CACHE.clear();
        SPRITE_CACHE_ACTIVE.clear();
    }

    /**
     * Shared draw path used by both the in-world BER and the item renderer.
     *
     * <p>Both go through {@code submitCustomGeometry}, which is 1.21.9's replacement for pulling a
     * buffer out of a {@code MultiBufferSource} and writing to it: the collector snapshots the
     * current pose and calls back at draw time with it and the vertex consumer for the render
     * type. LP's mesh emitter already worked against a {@code PoseStack.Pose}, so the callback
     * hands it straight through.</p>
     */
    public static void submitSolid(LogisticsSolidBlock block, PoseStack poseStack,
        SubmitNodeCollector collector, int light, int overlay) {
        TextureAtlasSprite icon = getIcon(block);
        EnumSet<SolidBlockModelParts.CoverSide> plates = block.hasCoverPlates()
            ? EnumSet.allOf(SolidBlockModelParts.CoverSide.class)
            : EnumSet.noneOf(SolidBlockModelParts.CoverSide.class);
        submit(poseStack, collector, icon, 0, plates, light, overlay);
    }

    private static void submit(PoseStack poseStack, SubmitNodeCollector collector,
        @Nullable TextureAtlasSprite icon, int rotation,
        EnumSet<SolidBlockModelParts.CoverSide> plates, int light, int overlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        if (parts.isEmpty() || icon == null) {
            return;
        }
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> {
            MeshRenderer.emit(buffer, pose, parts.body(rotation), icon, light, overlay);
            for (SolidBlockModelParts.CoverSide side : plates) {
                MeshRenderer.emit(buffer, pose, parts.outerPlate(side, rotation), icon, light, overlay);
                MeshRenderer.emit(buffer, pose, parts.innerPlate(side, rotation), icon, light, overlay);
            }
        });
    }

    @Override
    public SolidBlockRenderState createRenderState() {
        return new SolidBlockRenderState();
    }

    @Override
    public void extractRenderState(T be, SolidBlockRenderState state, float partialTicks, Vec3 cameraPos,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPos, breakProgress);
        state.block = null;
        state.icon = null;
        state.rotation = 0;
        state.plates.clear();

        Block block = be.getBlockState().getBlock();
        if (!(block instanceof LogisticsSolidBlock solidBlock)) {
            return;
        }
        state.block = solidBlock;

        if (!(be instanceof LogisticsSolidBlockEntity tile) || be.getLevel() == null) {
            // No tile to ask: fall back to the inventory look, all plates on.
            state.icon = getIcon(solidBlock);
            if (solidBlock.hasCoverPlates()) {
                state.plates.addAll(EnumSet.allOf(SolidBlockModelParts.CoverSide.class));
            }
            return;
        }

        state.icon = getIcon(solidBlock, tile.isActive());
        int rotation = tile.getRotation();
        state.rotation = rotation < 0 || rotation > 3 ? 0 : rotation;

        // LP1 hid the cover plates on sides where an adjacent LP pipe connects into this
        // block, so the pipe visually enters the machine.
        BlockPos pos = tile.getBlockPos();
        for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
            Direction facing = side.facing(state.rotation);
            BlockPos newPos = pos.relative(facing);
            BlockEntity sideTile = tile.getLevel().getBlockEntity(newPos);
            if (sideTile instanceof LogisticsTileGenericPipe tilePipe
                && tilePipe.visualState != null
                && tilePipe.visualState.pipeConnectionMatrix.isConnected(facing.getOpposite())) {
                continue;
            }
            state.plates.add(side);
        }
    }

    @Override
    public void submit(SolidBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState cameraState) {
        if (state.block == null) {
            return;
        }
        submit(poseStack, collector, state.icon, state.rotation, state.plates,
            state.lightCoords, OverlayTexture.NO_OVERLAY);
    }
}
