package logisticspipes.client.renderer.blockentity;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.solid.SolidBlockModelParts;
import logisticspipes.client.model.tube.TubeMeshes;
import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.client.renderer.item.LogisticsPipeItemRenderer;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe.SidedSign;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.transport.PipeTransportLogistics.BufferedItem;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public class LogisticsRenderPipe implements BlockEntityRenderer<LogisticsGenericPipeBlockEntity, PipeRenderState> {

    private static final int MAX_ITEMS_TO_RENDER = 10;
    /** The board a pipe sign is drawn on: the wall sign's block model, front face towards +z. */
    private static final BlockState SIGN_BOARD =
        Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH);
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final ItemStackRenderer itemRenderer = new ItemStackRenderer(0, 0, 0, false, false);
    @Nullable
    private static TextureAtlasSprite requestTableSprite = null;

    private final BlockModelResolver blockModels;
    /** Resolved on first use; a resource reload builds a new renderer, and with it a new state. */
    private final BlockModelRenderState signBoard = new BlockModelRenderState();
    private boolean signBoardResolved = false;

    public LogisticsRenderPipe(BlockEntityRendererProvider.Context context) {
        blockModels = context.blockModelResolver();
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

        collector.submitCustomGeometry(poseStack, LPRenderTypes.CUTOUT_BLOCK_SHEET, (pose, buffer) -> {
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

    /**
     * How far away a pipe is still drawn, from the player's own settings.
     *
     * <p>Read per frame rather than cached: the configuration screen edits the live value.
     */
    @Override
    public int getViewDistance() {
        return LPConfigs.CLIENT.RENDER_PIPE_DISTANCE.getAsInt();
    }

    @Override
    public void extractRenderState(LogisticsGenericPipeBlockEntity blockEntity, PipeRenderState state, float partialTicks,
        Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPos, breakProgress);
        state.blockEntity = blockEntity.pipe == null ? null : blockEntity;
        state.partialTicks = partialTicks;
        // The items traveling inside a pipe are the expensive half of this renderer, and they are
        // unreadable long before the pipe itself stops being visible, so they get their own
        // shorter distance.
        final int contentDistance = LPConfigs.CLIENT.RENDER_PIPE_CONTENT_DISTANCE.getAsInt();
        state.renderContent = Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(cameraPos)
            <= (double) contentDistance * contentDistance;
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
                OverlayTexture.NO_OVERLAY, state.renderContent);
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
    private void submitRequestTableBlock(PipeBlockRequestTable table, LogisticsGenericPipeBlockEntity pipeTile,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        submitRequestTableBaked(table, pipeTile, poseStack, collector, packedLight, packedOverlay);
    }

    /**
     * Draws the speedup tube's body, the one tube {@code PipeBakedModel} does not bake.
     *
     * <p>The other tubes have their textures stitched into the block atlas and live in the
     * chunk mesh. The speedup's 440x37 texture cannot join them without dropping the mip levels
     * of the whole atlas, and a {@code BakedQuad} can only reference an atlas sprite, so this
     * one keeps being emitted every frame.</p>
     */
    private void submitTubeGeometry(LogisticsGenericPipeBlockEntity blockEntity, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        if (TubeMeshes.keyFor(blockEntity.pipe) != null) {
            return;
        }
        TubeMeshes.TubeGeometry tube = TubeMeshes.forPipe(blockEntity.pipe);
        if (tube.isEmpty()) {
            return;
        }

        collector.submitCustomGeometry(poseStack, LPRenderTypes.TUBE_CUTOUT.apply(tube.texture()),
            (pose, buffer) -> MeshRenderer.emitRaw(buffer, pose, tube.mesh(), 0xFFFFFFFF, packedLight, packedOverlay));
    }

    /**
     * The request table on the mesh engine. It stays in the block entity renderer rather than
     * moving to a baked model: it is an {@code isPipeBlock()} pipe, so
     * {@code LogisticsNewRenderPipe} skips it and the pipe baked model has no geometry for it.
     * What changes here is that nothing is read from or written to a shared render state —
     * buffer, matrices, light and sprite are all arguments.
     */
    private void submitRequestTableBaked(PipeBlockRequestTable table, LogisticsGenericPipeBlockEntity pipeTile,
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
        collector.submitCustomGeometry(poseStack, LPRenderTypes.CUTOUT_BLOCK_SHEET, (pose, buffer) -> {
            MeshRenderer.emit(buffer, pose, parts.body(rot), sprite, packedLight, packedOverlay);
            for (SolidBlockModelParts.CoverSide side : SolidBlockModelParts.CoverSide.values()) {
                // Plates are skipped where a pipe connects, so adjacent pipes visually enter the
                // table rather than butting against a cover.
                if (pipeTile.visualState.pipeConnectionMatrix.isConnected(side.facing(rot))) {
                    continue;
                }
                MeshRenderer.emit(buffer, pose, parts.outerPlate(side, rot), sprite, packedLight, packedOverlay);
                MeshRenderer.emit(buffer, pose, parts.innerPlate(side, rot), sprite, packedLight, packedOverlay);
            }
        });
    }

    private void submitInternal(@Nullable LogisticsGenericPipeBlockEntity blockEntity, double x, double y, double z,
        float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay,
        boolean renderContent) {
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
            // The pipe frame and most tube bodies come from PipeBakedModel via the chunk mesh;
            // only the speedup tube still needs emitting here.
            if (!inHand) {
                submitTubeGeometry(blockEntity, poseStack, collector, packedLight, packedOverlay);
            }

            if (!inHand && !blockEntity.isOpaque() && renderContent) {
                if (blockEntity.pipe.transport instanceof PipeFluidTransportLogistics fluidTransport) {
                    PipeFluidRenderer.render(fluidTransport, poseStack, collector, packedLight, packedOverlay);
                }
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
            if (!item.getContainer().getBlockPos().equals(lPipe.getContainer().getBlockPos())) {
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
                    final BlockPos from = lPipe.getPos();
                    final BlockPos to = nPipe.getPos();
                    lX -= from.getX() - to.getX();
                    lY -= from.getY() - to.getY();
                    lZ -= from.getZ() - to.getZ();
                    lItemYaw += lPipe.transport.getYawDiff(item);
                    lPipe = nPipe;
                    item = item.renderCopy();
                    item.input = item.output;
                    item.output = null;
                } else {
                    continue;
                }
            }

            Vec3 pos = lPipe.getItemRenderPos(fPos, item);
            if (pos == null) {
                continue;
            }
            double boxScale = lPipe.getBoxRenderScale(fPos, item);
            double itemYaw = (lPipe.getItemRenderYaw(fPos, item) - lPipe.getItemRenderYaw(0, item) + lItemYaw) % 360;
            double itemPitch = lPipe.getItemRenderPitch(fPos, item);
            double itemYawForPitch = lPipe.getItemRenderYaw(fPos, item);

            ItemStack stack = item.getItemIdentifierStack().makeNormalStack();
            doRenderItem(stack, pipe.getContainer().getLevel(), lX + pos.x, lY + pos.y,
                lZ + pos.z, 0.75F, boxScale, itemYaw, itemPitch, itemYawForPitch, partialTickTime,
                poseStack, collector, packedLight, packedOverlay);
            count++;
        }

        count = 0;
        double dist = 0.135;
        Vec3 pos = new Vec3(0.5, 0.5, 0.5)
            .relative(Direction.SOUTH, dist)
            .relative(Direction.EAST, dist)
            .relative(Direction.UP, dist);
        for (BufferedItem item : pipe.transport.itemBuffer) {
            ItemStack stack = item.stack().makeNormalStack();
            doRenderItem(stack, pipe.getContainer().getLevel(), x + pos.x, y + pos.y,
                z + pos.z, 0.25F, 0, 0, 0, 0, partialTickTime, poseStack, collector, packedLight,
                packedOverlay);
            count++;
            if (count >= 27) {
                break;
            } else if (count % 9 == 0) {
                pos = pos.relative(Direction.SOUTH, dist * 2.0)
                    .relative(Direction.EAST, dist * 2.0)
                    .relative(Direction.DOWN, dist);
            } else if (count % 3 == 0) {
                pos = pos.relative(Direction.SOUTH, dist * 2.0)
                    .relative(Direction.WEST, dist);
            } else {
                pos = pos.relative(Direction.NORTH, dist);
            }
        }

        poseStack.popPose();
    }

    public void doRenderItem(ItemStack itemstack, @Nullable Level level, double x, double y, double z,
        float renderScale, double boxScale, double yaw, double pitch, double yawForPitch, float partialTickTime,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        boolean renderedFluid = TravelingItemBoxRenderer.render(itemstack, x, y, z, boxScale, yaw, pitch,
            yawForPitch, poseStack, collector, packedLight, packedOverlay);
        if (renderedFluid) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(renderScale, renderScale, renderScale);
        // Historic order: yaw around Y, then pitch around X after a secondary yawForPitch
        // rotation, matching the 1.12.2 glRotated sequence in CoreRoutedPipe-driven items.
        poseStack.rotateDegrees(Axis.YP, (float) yaw);
        poseStack.rotateDegrees(Axis.YP, (float) yawForPitch);
        poseStack.rotateDegrees(Axis.XP, (float) pitch);
        poseStack.rotateDegrees(Axis.YP, (float) -yawForPitch);
        // In 1.12.2 the -0.35 offset compensated for EntityItem's foot-to-center gap; in 1.20.1
        // ir.renderStatic(GROUND) has no such offset, so we leave the item centred in the pipe.
        itemRenderer.setItemstack(itemstack).setLevel(level).setPartialTickTime(partialTickTime);
        itemRenderer.renderInWorld(poseStack, collector, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private boolean needDistance(List<SidedSign> signs) {
        List<Direction> horizontal = signs.stream().map(SidedSign::side)
            .filter(side -> side.getAxis().isHorizontal()).toList();
        // Two signs on opposite sides are far enough apart already.
        return horizontal.size() > 2
            || horizontal.size() == 2 && horizontal.get(0) != horizontal.get(1).getOpposite();
    }

    private void submitPipeSigns(CoreRoutedPipe pipe, double x, double y, double z,
        PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        List<SidedSign> pipeSigns = pipe.getPipeSigns();
        if (pipe.getContainer() != null && !pipeSigns.isEmpty()) {
            for (SidedSign sidedSign : pipeSigns) {
                if (pipe.getContainer().visualState.pipeConnectionMatrix.isConnected(sidedSign.side())) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
                switch (sidedSign.side()) {
                    case UP:
                        poseStack.rotateDegrees(Axis.XP, 90);
                        break;
                    case DOWN:
                        poseStack.rotateDegrees(Axis.XP, -90);
                        break;
                    case NORTH:
                        // 0° yaw; no rotation required
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case SOUTH:
                        poseStack.rotateDegrees(Axis.YP, -180);
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case EAST:
                        poseStack.rotateDegrees(Axis.YP, -90);
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    case WEST:
                        poseStack.rotateDegrees(Axis.YP, 90);
                        if (needDistance(pipeSigns)) {
                            poseStack.translate(0.0F, 0.0F, -0.15F);
                        }
                        break;
                    default:
                }
                submitSign(sidedSign.sign(), poseStack, collector, packedLight);
                poseStack.popPose();
            }
        }
    }

    private void submitSign(IPipeSign sign, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        final float signScale = 2 / 3.0F;

        poseStack.translate(0.0F, -0.3125F, -0.36F);
        poseStack.rotate(Axis.YP, (float) Math.PI);

        // 26.2 turned the sign board into an ordinary block model: the entity model layer and the
        // signs atlas it was drawn from are gone. A wall sign hangs on its block, so it never has
        // the standing sign's post.
        if (!signBoardResolved) {
            blockModels.update(signBoard, SIGN_BOARD, BLOCK_DISPLAY_CONTEXT);
            signBoardResolved = true;
        }
        poseStack.pushPose();
        try {
            // The block model's board spans x 0..1, y 4.33..12.33 and z 0.33..1.67 sixteenths of
            // a block; the entity model, scaled by 2/3, put the same 1 x 0.5 x 1/12 board at
            // x -0.5..0.5, y 1/12..7/12, centred on z. This moves the one onto the other.
            poseStack.translate(-0.5F, -3 / 16F, -1 / 16F);
            signBoard.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        } finally {
            poseStack.popPose();
        }

        // Onto the front face of the board just drawn; the sign type positions its own text and
        // items from there.
        poseStack.translate(-0.32F, 0.5F * signScale + 0.08F, 0.07F * signScale);
        PipeSignRenderer.submit(sign, poseStack, collector, packedLight);
    }

    @Override
    public AABB getRenderBoundingBox(LogisticsGenericPipeBlockEntity blockEntity) {
        if (blockEntity.pipe == null) {
            return new AABB(blockEntity.getBlockPos()); // 1.20.1: AABB(BlockPos) creates the unit block cube
        }
        if (!blockEntity.pipe.isMultiBlock()) {
            return new AABB(blockEntity.getBlockPos()); // 1.20.1: AABB(BlockPos) creates the unit block cube
        } else {
            // The main block and the far corner of its own cube are in as well, so a multiblock
            // whose sub blocks all sit on one side still gets a box that contains the pipe itself.
            final BlockPos origin = blockEntity.getBlockPos();
            AABB box = new AABB(origin).move(0, 0, 0);
            for (CoreMultiBlockPipe.SubBlock sub : ((CoreMultiBlockPipe) blockEntity.pipe).getRotatedSubBlocks()) {
                box = box.minmax(new AABB(sub.at(origin)));
            }
            return box.inflate(1);
        }
    }
}
