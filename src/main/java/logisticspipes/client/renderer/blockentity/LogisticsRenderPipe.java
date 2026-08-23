package logisticspipes.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.data.AtlasIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Unit;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.client.model.tube.TubeMeshes;
import logisticspipes.client.renderer.item.LogisticsPipeItemRenderer;
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

public class LogisticsRenderPipe implements BlockEntityRenderer<LogisticsTileGenericPipe, PipeRenderState> {

    private static final int LIQUID_STAGES = 40;
    private static final int MAX_ITEMS_TO_RENDER = 10;
    /**
     * Depth left to an item drawn on a sign, as a fraction of its width. See {@link #renderItemStackOnSign}.
     */
    private static final float FLAT_ITEM_DEPTH = 0.02F;
    private static final WoodType TYPE = WoodType.OAK;
    private static final ItemStackRenderer itemRenderer = new ItemStackRenderer(0, 0, 0, false, false);
    public static LogisticsNewPipeItemBoxRenderer boxRenderer = new LogisticsNewPipeItemBoxRenderer();
    public static ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
    @Nullable
    private static TextureAtlasSprite requestTableSprite = null;

    private final Model.Simple signModel;
    /** Resolves a {@link Material} to its stitched sprite; 1.21.9 hands one to every BER. */
    private final MaterialSet materials;

    public LogisticsRenderPipe(BlockEntityRendererProvider.Context context) {
        // A pipe sign hangs on the pipe, so it never has the standing sign's post.
        signModel = SignRenderer.createSignModel(context.entityModelSet(), TYPE, false);
        materials = context.materials();
    }

    /**
     * LP1's {@code blocks/requesttable/requesttexture}, stitched into the block atlas by the
     * {@code blocks/} directory source. Looked up lazily because the atlas does not exist yet
     * when this class is loaded.
     */
    private static TextureAtlasSprite requestTableSprite() {
        if (requestTableSprite == null) {
            requestTableSprite = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(LPConstants.rl("blocks/requesttable/requesttexture"));
        }
        return requestTableSprite;
    }

    /**
     * Draws the Request Table's inventory form: the same solid-block body the placed table uses,
     * unrotated and with every cover plate present. Without this the item falls back to the pipe
     * geometry of {@link LogisticsPipeItemRenderer} and shows up as an ordinary pipe.
     */
    public static void submitRequestTableItem(PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight, int packedOverlay) {
        SolidBlockModelParts parts = PipeModelStore.solidBlock();
        TextureAtlasSprite sprite = requestTableSprite();
        if (parts.isEmpty() || sprite == null) {
            return;
        }

        collector.submitCustomGeometry(poseStack, RenderType.cutoutMipped(), (pose, buffer) -> {
            MeshRenderer.emit(buffer, pose, parts.body(0), sprite, packedLight, packedOverlay);
            for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
                MeshRenderer.emit(buffer, pose, parts.outerPlate(side, 0), sprite, packedLight, packedOverlay);
                MeshRenderer.emit(buffer, pose, parts.innerPlate(side, 0), sprite, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void extractRenderState(LogisticsTileGenericPipe blockEntity, PipeRenderState state, float partialTicks,
        Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPos, breakProgress);
        state.blockEntity = blockEntity.pipe == null ? null : blockEntity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(PipeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState cameraState) {
        if (state.blockEntity == null) {
            return;
        }

        // submitInternal draws what genuinely varies per frame: pipe signs, traveling items and
        // their transport boxes, the request table body and fluid overlays. The pipe frame
        // itself now lives in the chunk mesh, supplied by PipeBakedModel.
        poseStack.pushPose();
        try {
            submitInternal(state.blockEntity, 0, 0, 0, state.partialTicks, poseStack, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY);
        } finally {
            poseStack.popPose();
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
    private void submitRequestTableBlock(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        submitRequestTableBaked(table, pipeTile, poseStack, collector, packedLight, packedOverlay);
    }

    /**
     * Draws the high-speed tube body.
     *
     * <p>Unlike the pipe frame, this cannot move into the chunk mesh: the tubes are textured
     * with standalone PNGs rather than sprites stitched into the block atlas, and a
     * {@code BakedQuad} can only reference an atlas sprite. So tubes keep immediate-mode
     * rendering — but on the mesh engine, with no shared render state.</p>
     */
    private void submitTubeGeometry(LogisticsTileGenericPipe blockEntity, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        TubeMeshes.TubeGeometry tube = TubeMeshes.forPipe(blockEntity.pipe);
        if (tube.isEmpty()) {
            return;
        }

        collector.submitCustomGeometry(poseStack, RenderType.entityCutoutNoCull(tube.texture()),
            (pose, buffer) -> MeshRenderer.emitRaw(buffer, pose, tube.mesh(), 0xFFFFFFFF, packedLight, packedOverlay));
    }

    /**
     * The request table on the mesh engine. It stays in the block entity renderer rather than
     * moving to a baked model: it is an {@code isPipeBlock()} pipe, so
     * {@code LogisticsNewRenderPipe} skips it and the pipe baked model has no geometry for it.
     * What changes here is that nothing is read from or written to a shared render state —
     * buffer, matrices, light and sprite are all arguments.
     */
    private void submitRequestTableBaked(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
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

        final int rot = rotation;
        collector.submitCustomGeometry(poseStack, RenderType.cutoutMipped(), (pose, buffer) -> {
            MeshRenderer.emit(buffer, pose, parts.body(rot), sprite, packedLight, packedOverlay);
            for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
                // Plates are skipped where a pipe connects, so adjacent pipes visually enter the
                // table rather than butting against a cover.
                if (pipeTile.renderState.pipeConnectionMatrix.isConnected(side.facing(rot))) {
                    continue;
                }
                MeshRenderer.emit(buffer, pose, parts.outerPlate(side, rot), sprite, packedLight, packedOverlay);
                MeshRenderer.emit(buffer, pose, parts.innerPlate(side, rot), sprite, packedLight, packedOverlay);
            }
        });
    }

    private void submitInternal(@Nullable LogisticsTileGenericPipe blockEntity, double x, double y, double z,
        float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        // In 1.20.1 the BER PoseStack is pre-translated, so we always pass (0,0,0).
        // Use blockEntity==null as the sole in-hand signal instead.
        boolean inHand = (blockEntity == null);
        if (!inHand && blockEntity.pipe == null) {
            return;
        }

        // 1.20.1: depth + rescale-normal + colour state are managed by the bound RenderType,
        // so the old GlStateManager._enableDepthTest/_depthFunc/_depthMask block is gone.
        // destroyStage overlay is handled by the outer BER pipeline (crumbling buffer) and
        // no longer needs a per-pipe matrix push here.

        poseStack.pushPose();
        try {
            if (!inHand && blockEntity.pipe instanceof CoreRoutedPipe) {
                submitPipeSigns((CoreRoutedPipe) blockEntity.pipe, x, y, z, poseStack, collector,
                    packedLight);
            }

            // The Request Table is an isPipeBlock() pipe, so the pipe baked model holds no
            // geometry for it and the 1.12 ISimpleBlockRenderingHandler that drew its block
            // body was never ported: without this branch the table is invisible.
            if (!inHand && blockEntity.pipe instanceof PipeBlockRequestTable) {
                submitRequestTableBlock((PipeBlockRequestTable) blockEntity.pipe, blockEntity, poseStack, collector,
                    packedLight, packedOverlay);
            }
            // The pipe frame comes from PipeBakedModel via the chunk mesh; only the tube
            // bodies still need emitting here, because their textures are standalone PNGs
            // rather than atlas sprites and so cannot be baked.
            if (!inHand) {
                submitTubeGeometry(blockEntity, poseStack, collector, packedLight, packedOverlay);
            }

            if (!inHand && !blockEntity.isOpaque()) {
                if (blockEntity.pipe.transport != null) {
                    submitSolids(blockEntity.pipe, x, y, z, partialTicks, poseStack, collector, packedLight,
                        packedOverlay);
                }
            }
        } finally {
            poseStack.popPose();
        }
        // MCMP special renderer removed — MCMultiPart has no 1.20.1 port (former dummy was a no-op).
    }

    private void submitSolids(CoreUnroutedPipe pipe, double x, double y, double z, float partialTickTime,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
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
                poseStack, collector, packedLight, packedOverlay);
            count++;
        }

        count = 0;
        double dist = 0.135;
        DoubleCoordinates pos = new DoubleCoordinates(0.5, 0.5, 0.5);
        CoordinateUtils.add(pos, Direction.SOUTH, dist);
        CoordinateUtils.add(pos, Direction.EAST, dist);
        CoordinateUtils.add(pos, Direction.UP, dist);
        for (Pair<ItemIdentifierStack, Pair<Integer, Integer>> item : pipe.transport.itemBuffer) {
            if (item == null || item.getValue1() == null) {
                continue;
            }
            ItemStack stack = item.getValue1().makeNormalStack();
            doRenderItem(stack, pipe.container.getWorld(), x + pos.getXCoord(), y + pos.getYCoord(),
                z + pos.getZCoord(), light, 0.25F, 0, 0, 0, 0, partialTickTime, poseStack, collector, packedLight,
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

    public void doRenderItem(ItemStack itemstack, @Nullable Level level, double x, double y, double z, float light,
        float renderScale, double boxScale, double yaw, double pitch, double yawForPitch, float partialTickTime,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        LogisticsRenderPipe.boxRenderer.doRenderItem(itemstack, light, x, y, z, boxScale, yaw, pitch, yawForPitch,
            poseStack, collector, packedLight, packedOverlay);

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
        itemRenderer.setItemstack(itemstack).setLevel(level).setPartialTickTime(partialTickTime);
        itemRenderer.renderInWorld(poseStack, collector, packedLight, packedOverlay);
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

    private void submitPipeSigns(CoreRoutedPipe pipe, double x, double y, double z,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
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
                submitSign(pipe, pair.getValue2(), poseStack, collector, packedLight);
                poseStack.popPose();
            }
        }
    }

    private void submitSign(CoreRoutedPipe pipe, IPipeSign type, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        final float signScale = 2 / 3.0F;

        poseStack.translate(0.0F, -0.3125F, -0.36F);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.PI));

        poseStack.pushPose();
        try {
            poseStack.scale(signScale, -signScale, -signScale);
            // Models are submitted whole now instead of being rendered into a buffer picked from
            // the material; the material only supplies the render type and the stitched sprite.
            Material material = Sheets.getSignMaterial(TYPE);
            collector.submitModel(signModel, Unit.INSTANCE, poseStack,
                material.renderType(signModel::renderType), packedLight, OverlayTexture.NO_OVERLAY,
                -1, materials.get(material), 0, null);
        } finally {
            poseStack.popPose();
        }

        // Onto the front face of the board just drawn; the sign type positions its own text and
        // items from there.
        poseStack.translate(-0.32F, 0.5F * signScale + 0.08F, 0.07F * signScale);
        type.render(pipe, this, poseStack, collector, packedLight);
    }

    public void renderItemStackOnSign(ItemStack itemstack, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        if (itemstack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Position the item onto the front face of the sign and scale it down to fit.
        poseStack.translate(0.0F, 0.0F, 0.0F);
        // Flattened along the sign's normal, which is what makes a block item read as a picture
        // on the plank rather than a cube sticking out of it. The GUI display context has
        // already put the model in its inventory pose by the time this scale applies, so
        // collapsing depth here leaves exactly the inventory icon's silhouette, drawn in the
        // plane of the sign. Not zero: the faces still need distinct depths to sort against
        // each other, they just need a thickness nobody can see.
        poseStack.scale(0.25F, 0.25F, 0.25F * FLAT_ITEM_DEPTH);
        // renderStatic is gone: resolve the stack to a render state and submit it.
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver()
            .updateForTopItem(renderState, itemstack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        renderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
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
