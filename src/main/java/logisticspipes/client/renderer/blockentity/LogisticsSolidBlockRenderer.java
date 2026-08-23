package logisticspipes.client.renderer.blockentity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.data.AtlasIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

/**
 * Shared BlockEntityRenderer for all LP solid blocks. Reuses the OBJ-parsed 3D body
 * and 5 cover plates held by {@link SolidBlockModelParts} and renders them to the
 * cutoutMipped buffer via {@link MeshRenderer}.
 *
 * <p>Each {@link LogisticsSolidBlock.Type} maps to a sprite at
 * {@code logisticspipes:solid_block/<name>} which is used as the plate texture.</p>
 */
public class LogisticsSolidBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, SolidBlockRenderState> {

    private static final Map<LogisticsSolidBlock.Type, TextureAtlasSprite> SPRITE_CACHE =
        new EnumMap<>(LogisticsSolidBlock.Type.class);
    private static final Map<LogisticsSolidBlock.Type, TextureAtlasSprite> SPRITE_CACHE_ACTIVE =
        new EnumMap<>(LogisticsSolidBlock.Type.class);

    public LogisticsSolidBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static String textureNameFor(LogisticsSolidBlock.Type type) {
        return switch (type) {
            case LOGISTICS_POWER_JUNCTION -> "power_junction";
            case LOGISTICS_SECURITY_STATION -> "security_station";
            case LOGISTICS_AUTOCRAFTING_TABLE -> "crafting_table";
            case LOGISTICS_FUZZYCRAFTING_TABLE -> "crafting_table_fuzzy";
            case LOGISTICS_STATISTICS_TABLE -> "statistics_table";
            case LOGISTICS_RF_POWERPROVIDER -> "power_provider_rf";
            case LOGISTICS_PROGRAM_COMPILER -> "program_compiler";
            default -> "frame";
        };
    }

    public static TextureAtlasSprite getIcon(LogisticsSolidBlock.Type type) {
        return getIcon(type, false);
    }

    /**
     * LP1: types with an active texture switch to {@code <name>_active} while the tile is active.
     */
    public static TextureAtlasSprite getIcon(LogisticsSolidBlock.Type type, boolean active) {
        boolean useActive = active && type.isHasActiveTexture();
        Map<LogisticsSolidBlock.Type, TextureAtlasSprite> cache = useActive ? SPRITE_CACHE_ACTIVE : SPRITE_CACHE;
        TextureAtlasSprite cached = cache.get(type);
        if (cached != null) {
            return cached;
        }
        String name = textureNameFor(type) + (useActive ? "_active" : "");
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getAtlasManager()
            .getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(LPConstants.rl("solid_block/" + name));
        cache.put(type, sprite);
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
    public static void submitSolid(LogisticsSolidBlock.Type type, PoseStack poseStack,
        SubmitNodeCollector collector, int light, int overlay) {
        TextureAtlasSprite icon = getIcon(type);
        // The frame draws no cover plates in the inventory render; mirror that.
        EnumSet<SolidBlockModelParts.CoverSide> plates = type == LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME
            ? EnumSet.noneOf(SolidBlockModelParts.CoverSide.class)
            : EnumSet.allOf(SolidBlockModelParts.CoverSide.class);
        submit(poseStack, collector, icon, 0, plates, light, overlay);
    }

    private static void submit(PoseStack poseStack, SubmitNodeCollector collector,
        @Nullable TextureAtlasSprite icon, int rotation,
        EnumSet<SolidBlockModelParts.CoverSide> plates, int light, int overlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        if (parts.isEmpty() || icon == null) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderType.cutoutMipped(), (pose, buffer) -> {
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
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPos, breakProgress);
        state.type = null;
        state.icon = null;
        state.rotation = 0;
        state.plates.clear();

        Block block = be.getBlockState().getBlock();
        if (!(block instanceof LogisticsSolidBlock solidBlock)) {
            return;
        }
        LogisticsSolidBlock.Type type = solidBlock.getType();
        state.type = type;

        if (!(be instanceof LogisticsSolidBlockEntity tile) || be.getLevel() == null) {
            // No tile to ask: fall back to the inventory look, all plates on.
            state.icon = getIcon(type);
            if (type != LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME) {
                state.plates.addAll(EnumSet.allOf(SolidBlockModelParts.CoverSide.class));
            }
            return;
        }

        state.icon = getIcon(type, tile.isActive());
        int rotation = tile.getRotation();
        state.rotation = rotation < 0 || rotation > 3 ? 0 : rotation;

        // LP1 hid the cover plates on sides where an adjacent LP pipe connects into this
        // block, so the pipe visually enters the machine.
        DoubleCoordinates pos = new DoubleCoordinates(tile);
        for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
            Direction facing = side.facing(state.rotation);
            DoubleCoordinates newPos = CoordinateUtils.sum(pos, facing);
            BlockEntity sideTile = newPos.getTileEntity(tile.getLevel());
            if (sideTile instanceof LogisticsTileGenericPipe tilePipe
                && tilePipe.renderState != null
                && tilePipe.renderState.pipeConnectionMatrix.isConnected(facing.getOpposite())) {
                continue;
            }
            state.plates.add(side);
        }
    }

    @Override
    public void submit(SolidBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState cameraState) {
        if (state.type == null) {
            return;
        }
        submit(poseStack, collector, state.icon, state.rotation, state.plates,
            state.lightCoords, OverlayTexture.NO_OVERLAY);
    }
}
