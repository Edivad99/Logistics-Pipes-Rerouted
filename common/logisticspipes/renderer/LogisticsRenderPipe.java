package logisticspipes.renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.client.model.tube.TubeMeshes;
import logisticspipes.client.renderer.blockentity.LogisticsSolidBlockRenderer;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.renderer.newpipe.LogisticsNewPipeItemBoxRenderer;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

public class LogisticsRenderPipe implements BlockEntityRenderer<LogisticsTileGenericPipe> {

    private static final ExecutorService pool = Executors.newFixedThreadPool(1);
    private static final int LIQUID_STAGES = 40;
    private static final int MAX_ITEMS_TO_RENDER = 10;
    private static final ResourceLocation SIGN = ResourceLocation.withDefaultNamespace("textures/entity/sign.png");
    public static LogisticsNewPipeItemBoxRenderer boxRenderer = new LogisticsNewPipeItemBoxRenderer();
    public static ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
    private static final ItemStackRenderer itemRenderer = new ItemStackRenderer(0, 0, 0, false, false);

    public LogisticsRenderPipe(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LogisticsTileGenericPipe tileentity, float partialTicks, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (tileentity.pipe == null) {
            return;
        }

        // renderInternal draws what genuinely varies per frame: pipe signs, traveling items and
        // their transport boxes, the request table body and fluid overlays. The pipe frame
        // itself now lives in the chunk mesh, supplied by PipeBakedModel.
        poseStack.pushPose();
        try {
            renderInternal(tileentity, 0, 0, 0, partialTicks, -1, 1.0f, poseStack, bufferSource, packedLight,
                packedOverlay);
        } finally {
            poseStack.popPose();
        }
    }

    @Nullable
    private static TextureAtlasSprite requestTableSprite = null;

    /**
     * LP1's {@code blocks/requesttable/requesttexture}, stitched into the block atlas by the
     * {@code blocks/} directory source. Looked up lazily because the atlas does not exist yet
     * when this class is loaded.
     */
    @Nullable
    private static TextureAtlasSprite requestTableSprite() {
        if (requestTableSprite == null) {
            requestTableSprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(LPConstants.rl("blocks/requesttable/requesttexture"));
        }
        return requestTableSprite;
    }

    /**
     * Draws the Request Table's inventory form: the same solid-block body the placed table uses,
     * unrotated and with every cover plate present. Without this the item falls back to the pipe
     * geometry of {@link LogisticsPipeItemRenderer} and shows up as an ordinary pipe.
     */
    public static void renderRequestTableItem(PoseStack poseStack, MultiBufferSource bufferSource,
        int packedLight, int packedOverlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        TextureAtlasSprite sprite = requestTableSprite();
        if (parts.isEmpty() || sprite == null) {
            return;
        }

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
        MeshRenderer.emit(buffer, poseStack.last(), parts.body(0), sprite, packedLight, packedOverlay);
        for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
            MeshRenderer.emit(buffer, poseStack.last(), parts.outerPlate(side, 0), sprite, packedLight, packedOverlay);
            MeshRenderer.emit(buffer, poseStack.last(), parts.innerPlate(side, 0), sprite, packedLight, packedOverlay);
        }
    }

    /**
     * Draws the Request Table's full block body. Port of the dead 1.12
     * LogisticsNewPipeWorldRenderer request-table branch onto the
     * {@link LogisticsSolidBlockRenderer#renderSolid} draw path: the shared solid-block OBJ
     * body plus cover plates, with plates omitted on connected sides and the body rotated
     * to the table's facing. Texture is LP1's {@code blocks/requesttable/requesttexture}
     * (stitched into the block atlas by the {@code blocks/} directory source).
     */
    private void renderRequestTableBlock(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderRequestTableBaked(table, pipeTile, poseStack, bufferSource, packedLight, packedOverlay);
    }

    /**
     * Draws the high-speed tube body.
     *
     * <p>Unlike the pipe frame, this cannot move into the chunk mesh: the tubes are textured
     * with standalone PNGs rather than sprites stitched into the block atlas, and a
     * {@code BakedQuad} can only reference an atlas sprite. So tubes keep immediate-mode
     * rendering — but on the mesh engine, with no shared render state.</p>
     */
    private void renderTubeGeometry(LogisticsTileGenericPipe tileentity, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        TubeMeshes.TubeGeometry tube = TubeMeshes.forPipe(tileentity.pipe);
        if (tube.isEmpty()) {
            return;
        }

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(tube.texture()));
        MeshRenderer.emitRaw(buffer, poseStack.last(),
            tube.mesh(), 0xFFFFFFFF, packedLight, packedOverlay);
    }

    /**
     * The request table on the mesh engine. It stays in the block entity renderer rather than
     * moving to a baked model: it is an {@code isPipeBlock()} pipe, so
     * {@code LogisticsNewRenderPipe} skips it and the pipe baked model has no geometry for it.
     * What changes here is that nothing is read from or written to a shared render state —
     * buffer, matrices, light and sprite are all arguments.
     */
    private void renderRequestTableBaked(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        if (parts.isEmpty()) {
            return;
        }

        TextureAtlasSprite sprite = requestTableSprite();
        if (sprite == null) {
            return;
        }

        int rotation = table.getRotation();
        if (rotation < 0 || rotation > 3) {
            rotation = 0;
        }

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
        MeshRenderer.emit(buffer, poseStack.last(),
            parts.body(rotation), sprite, packedLight, packedOverlay);

        for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
            // Plates are skipped where a pipe connects, so adjacent pipes visually enter the
            // table rather than butting against a cover.
            if (pipeTile.renderState.pipeConnectionMatrix.isConnected(side.facing(rotation))) {
                continue;
            }
            MeshRenderer.emit(buffer, poseStack.last(),
                parts.outerPlate(side, rotation), sprite, packedLight, packedOverlay);
            MeshRenderer.emit(buffer, poseStack.last(),
                parts.innerPlate(side, rotation), sprite, packedLight, packedOverlay);
        }
    }

    private void renderInternal(@Nullable LogisticsTileGenericPipe tileentity, double x, double y, double z,
        float partialTicks, int destroyStage, float alpha,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // In 1.20.1 the BER PoseStack is pre-translated, so we always pass (0,0,0).
        // Use tileentity==null as the sole in-hand signal instead.
        boolean inHand = (tileentity == null);
        if (!inHand && tileentity.pipe == null) {
            return;
        }

        // 1.20.1: depth + rescale-normal + colour state are managed by the bound RenderType,
        // so the old GlStateManager._enableDepthTest/_depthFunc/_depthMask block is gone.
        // destroyStage overlay is handled by the outer BER pipeline (crumbling buffer) and
        // no longer needs a per-pipe matrix push here.

        poseStack.pushPose();
        try {
            if (!inHand && tileentity.pipe instanceof CoreRoutedPipe) {
                renderPipeSigns((CoreRoutedPipe) tileentity.pipe, x, y, z, partialTicks, poseStack, bufferSource,
                    packedLight, packedOverlay);
            }

            double distance = !inHand ?
                new DoubleCoordinates((BlockEntity) tileentity).distanceTo(
                    new DoubleCoordinates(Minecraft.getInstance().player)) :
                0;

            // The Request Table is an isPipeBlock() pipe, so the pipe baked model holds no
            // geometry for it and the 1.12 ISimpleBlockRenderingHandler that drew its block
            // body was never ported: without this branch the table is invisible.
            if (!inHand && tileentity.pipe instanceof PipeBlockRequestTable) {
                renderRequestTableBlock((PipeBlockRequestTable) tileentity.pipe, tileentity, poseStack, bufferSource,
                    packedLight, packedOverlay);
            }
            // The pipe frame comes from PipeBakedModel via the chunk mesh; only the tube
            // bodies still need emitting here, because their textures are standalone PNGs
            // rather than atlas sprites and so cannot be baked.
            if (!inHand) {
                renderTubeGeometry(tileentity, poseStack, bufferSource, packedLight, packedOverlay);
            }

            if (!inHand && !tileentity.isOpaque()) {
                if (tileentity.pipe.transport != null) {
                    renderSolids(tileentity.pipe, x, y, z, partialTicks, poseStack, bufferSource, packedLight,
                        packedOverlay);
                }
            }
        } finally {
            poseStack.popPose();
        }
        // MCMP special renderer removed — MCMultiPart has no 1.20.1 port (former dummy was a no-op).
    }

    private void renderSolids(CoreUnroutedPipe pipe, double x, double y, double z, float partialTickTime,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        float light = 1.0F; // full-bright; actual lighting applied via packedLight parameter

        int count = 0;
        for (LPTravelingItem item : pipe.transport.items) {
            CoreUnroutedPipe lPipe = pipe;
            double lX = x;
            double lY = y;
            double lZ = z;
            float lItemYaw = item.getYaw();
            if (count >= LogisticsRenderPipe.MAX_ITEMS_TO_RENDER) {
                break;
            }

            if (item.getItemIdentifierStack() == null) {
                continue;
            }
            if (!item.getContainer().getBlockPos().equals(lPipe.container.getBlockPos())) {
                continue;
            }

            if (item.getPosition() > lPipe.transport.getPipeLength() || item.getPosition() < 0) {
                continue;
            }

            float fPos = item.getPosition() + item.getSpeed() * partialTickTime;
            if (fPos > lPipe.transport.getPipeLength() && item.output != null) {
                CoreUnroutedPipe nPipe = lPipe.transport.getNextPipe(item.output);
                if (nPipe != null) {
                    fPos -= lPipe.transport.getPipeLength();
                    lX -= lPipe.getX() - nPipe.getX();
                    lY -= lPipe.getY() - nPipe.getY();
                    lZ -= lPipe.getZ() - nPipe.getZ();
                    lItemYaw += lPipe.transport.getYawDiff(item);
                    lPipe = nPipe;
                    item = item.renderCopy();
                    item.input = item.output;
                    item.output = null;
                } else {
                    continue;
                }
            }

            DoubleCoordinates pos = lPipe.getItemRenderPos(fPos, item);
            if (pos == null) {
                continue;
            }
            double boxScale = lPipe.getBoxRenderScale(fPos, item);
            double itemYaw = (lPipe.getItemRenderYaw(fPos, item) - lPipe.getItemRenderYaw(0, item) + lItemYaw) % 360;
            double itemPitch = lPipe.getItemRenderPitch(fPos, item);
            double itemYawForPitch = lPipe.getItemRenderYaw(fPos, item);

            ItemStack stack = item.getItemIdentifierStack().makeNormalStack();
            doRenderItem(stack, pipe.container.getWorld(), lX + pos.getXCoord(), lY + pos.getYCoord(),
                lZ + pos.getZCoord(), light, 0.75F, boxScale, itemYaw, itemPitch, itemYawForPitch, partialTickTime,
                poseStack, bufferSource, packedLight, packedOverlay);
            count++;
        }

        count = 0;
        double dist = 0.135;
        DoubleCoordinates pos = new DoubleCoordinates(0.5, 0.5, 0.5);
        CoordinateUtils.add(pos, Direction.SOUTH, dist);
        CoordinateUtils.add(pos, Direction.EAST, dist);
        CoordinateUtils.add(pos, Direction.UP, dist);
        for (Pair<ItemIdentifierStack, Pair<Integer, Integer>> item : pipe.transport._itemBuffer) {
            if (item == null || item.getValue1() == null) {
                continue;
            }
            ItemStack stack = item.getValue1().makeNormalStack();
            doRenderItem(stack, pipe.container.getWorld(), x + pos.getXCoord(), y + pos.getYCoord(),
                z + pos.getZCoord(), light, 0.25F, 0, 0, 0, 0, partialTickTime, poseStack, bufferSource, packedLight,
                packedOverlay);
            count++;
            if (count >= 27) {
                break;
            } else if (count % 9 == 0) {
                CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
                CoordinateUtils.add(pos, Direction.EAST, dist * 2.0);
                CoordinateUtils.add(pos, Direction.DOWN, dist);
            } else if (count % 3 == 0) {
                CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
                CoordinateUtils.add(pos, Direction.WEST, dist);
            } else {
                CoordinateUtils.add(pos, Direction.NORTH, dist);
            }
        }

        poseStack.popPose();
    }

    public void doRenderItem(ItemStack itemstack, Level world, double x, double y, double z, float light,
        float renderScale, double boxScale, double yaw, double pitch, double yawForPitch, float partialTickTime,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LogisticsRenderPipe.boxRenderer.doRenderItem(itemstack, light, x, y, z, boxScale, yaw, pitch, yawForPitch,
            poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(renderScale, renderScale, renderScale);
        // Historic order: yaw around Y, then pitch around X after a secondary yawForPitch
        // rotation, matching the 1.12.2 glRotated sequence in CoreRoutedPipe-driven items.
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yaw)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
        // In 1.12.2 the -0.35 offset compensated for EntityItem's foot-to-center gap; in 1.20.1
        // ir.renderStatic(GROUND) has no such offset, so we leave the item centred in the pipe.
        itemRenderer.setItemstack(itemstack).setWorld(world).setPartialTickTime(partialTickTime);
        itemRenderer.renderInWorld(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private boolean needDistance(List<Pair<Direction, IPipeSign>> list) {
        List<Pair<Direction, IPipeSign>> copy = new ArrayList<>(list);
        Iterator<Pair<Direction, IPipeSign>> iter = copy.iterator();
        boolean north = false, south = false, east = false, west = false;
        while (iter.hasNext()) {
            Pair<Direction, IPipeSign> pair = iter.next();
            if (pair.getValue1() == Direction.UP || pair.getValue1() == Direction.DOWN || pair.getValue1() == null) {
                iter.remove();
            }
            if (pair.getValue1() == Direction.NORTH) {
                north = true;
            }
            if (pair.getValue1() == Direction.SOUTH) {
                south = true;
            }
            if (pair.getValue1() == Direction.EAST) {
                east = true;
            }
            if (pair.getValue1() == Direction.WEST) {
                west = true;
            }
        }
        boolean result = copy.size() > 1;
        if (copy.size() == 2) {
            if (north && south) {
                result = false;
            }
            if (east && west) {
                result = false;
            }
        }
        return result;
    }

    private void renderPipeSigns(CoreRoutedPipe pipe, double x, double y, double z, float partialTickTime,
        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        List<Pair<Direction, IPipeSign>> pipeSigns = pipe.getPipeSigns();
        if (pipe.container != null && !pipeSigns.isEmpty()) {
            for (Pair<Direction, IPipeSign> pair : pipeSigns) {
                if (pipe.container.renderState.pipeConnectionMatrix.isConnected(pair.getValue1())) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
                switch (pair.getValue1()) {
                    case UP:
                        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90)));
                        break;
                    case DOWN:
                        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-90)));
                        break;
                    case NORTH:
                        // 0° yaw; no rotation required
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case SOUTH:
                        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-180)));
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case EAST:
                        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-90)));
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case WEST:
                        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(90)));
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    default:
                }
                renderSign(pipe, pair.getValue2(), partialTickTime, poseStack, bufferSource, packedLight);
                poseStack.popPose();
            }
        }
    }

    private void renderSign(CoreRoutedPipe pipe, IPipeSign type, float partialTickTime, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight) {
        // ModelSign background rendering deferred; delegate text/item rendering to the sign.
        type.render(pipe, this, poseStack, bufferSource, packedLight);
    }

    private void resetStateManager() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public void renderItemStackOnSign(ItemStack itemstack) {
        // Legacy no-arg stub — rendering deferred. Use the PoseStack overload instead.
    }

    public void renderItemStackOnSign(ItemStack itemstack, PoseStack poseStack, MultiBufferSource bufferSource,
        int packedLight) {
        if (itemstack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Position the item onto the front face of the sign and scale it down to fit.
        poseStack.translate(0.0F, 0.08F, 0.0F);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        Level level = Minecraft.getInstance().level;
        Minecraft.getInstance().getItemRenderer().renderStatic(
            itemstack,
            ItemDisplayContext.FIXED,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            level,
            0);
        poseStack.popPose();
    }

    public String cut(String name, Font renderer) {
        if (renderer.width(name) < 90) {
            return name;
        }
        StringBuilder sum = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (renderer.width(sum.toString() + name.charAt(i) + "...") < 90) {
                sum.append(name.charAt(i));
            } else {
                return sum + "...";
            }
        }
        return sum.toString();
    }

    @Override
    public AABB getRenderBoundingBox(LogisticsTileGenericPipe blockEntity) {
        if (blockEntity.pipe == null) {
            return new AABB(blockEntity.getBlockPos()); // 1.20.1: AABB(BlockPos) creates the unit block cube
        }
        if (!blockEntity.pipe.isMultiBlock()) {
            return new AABB(blockEntity.getBlockPos()); // 1.20.1: AABB(BlockPos) creates the unit block cube
        } else {
            LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> set = ((CoreMultiBlockPipe) blockEntity.pipe).getRotatedSubBlocks();
            set.addToAll(blockEntity.pipe.getLPPosition());
            set.add(new DoubleCoordinatesType<>(blockEntity.getBlockPos(),
                CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
            set.add(
                new DoubleCoordinatesType<>(blockEntity.getBlockPos().getX() + 1, blockEntity.getBlockPos().getY() + 1,
                    blockEntity.getBlockPos().getZ() + 1, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
            return new AABB(set.getMinXD() - 1, set.getMinYD() - 1, set.getMinZD() - 1, set.getMaxXD() + 1,
                set.getMaxYD() + 1, set.getMaxZD() + 1);
        }
    }
}
