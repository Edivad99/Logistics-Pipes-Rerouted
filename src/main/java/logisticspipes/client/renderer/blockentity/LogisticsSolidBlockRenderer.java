package logisticspipes.client.renderer.blockentity;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.blocks.LogisticsSolidBlock;
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
public class LogisticsSolidBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

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
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(LPConstants.rl("solid_block/" + name));
        cache.put(type, sprite);
        return sprite;
    }

    public static void clearCache() {
        SPRITE_CACHE.clear();
        SPRITE_CACHE_ACTIVE.clear();
    }

    /**
     * Shared draw path used by both the in-world BER and the item BEWLR.
     */
    public static void renderSolid(LogisticsSolidBlock.Type type, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        TextureAtlasSprite icon = getIcon(type);
        if (parts.isEmpty() || icon == null) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.cutoutMipped());
        MeshRenderer.emit(buffer, pose.last(), parts.body(0), icon, light, overlay);

        // The frame draws no cover plates in the inventory render; mirror that.
        if (type != LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME) {
            for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
                MeshRenderer.emit(buffer, pose.last(), parts.outerPlate(side, 0), icon, light, overlay);
                MeshRenderer.emit(buffer, pose.last(), parts.innerPlate(side, 0), icon, light, overlay);
            }
        }
    }

    @Override
    public void render(T be, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        Block block = be.getBlockState().getBlock();
        if (!(block instanceof LogisticsSolidBlock)) {
            return;
        }
        LogisticsSolidBlock.Type type = ((LogisticsSolidBlock) block).getType();
        if (!(be instanceof LogisticsSolidBlockEntity tile) || be.getLevel() == null) {
            renderSolid(type, pose, buffers, light, overlay);
            return;
        }

        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        TextureAtlasSprite icon = getIcon(type, tile.isActive());
        if (parts.isEmpty() || icon == null) {
            return;
        }

        int rotation = tile.getRotation();
        if (rotation < 0 || rotation > 3) {
            rotation = 0;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.cutoutMipped());
        MeshRenderer.emit(buffer, pose.last(), parts.body(rotation), icon, light, overlay);

        // LP1 hid the cover plates on sides where an adjacent LP pipe connects into this
        // block, so the pipe visually enters the machine.
        DoubleCoordinates pos = new DoubleCoordinates(tile);
        for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
            DoubleCoordinates newPos = CoordinateUtils.sum(pos, side.facing(rotation));
            BlockEntity sideTile = newPos.getTileEntity(tile.getLevel());
            if (sideTile instanceof LogisticsTileGenericPipe tilePipe
                && tilePipe.renderState != null
                && tilePipe.renderState.pipeConnectionMatrix.isConnected(side.facing(rotation).getOpposite())) {
                continue;
            }
            MeshRenderer.emit(buffer, pose.last(), parts.outerPlate(side, rotation), icon, light, overlay);
            MeshRenderer.emit(buffer, pose.last(), parts.innerPlate(side, rotation), icon, light, overlay);
        }
    }
}
