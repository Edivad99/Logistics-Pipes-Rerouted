package logisticspipes.ticks;

import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.GuiOverlay;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.tube.TubeMeshes;
import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.client.renderer.QuickSortMarkerOverlay;
import logisticspipes.client.renderer.hud.LogisticsHUDRenderer;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.world.item.ItemLogisticsPipe;

/**
 * The client's per-frame work: the HUD, the overlays drawn over screens, and the see-through
 * preview of the pipe the player is about to place.
 */
public class RenderTickHandler {

    private static final Identifier GHOST_PIPE_TEXTURE = LPConstants.rl("textures/blocks/pipes/white.png");

    /**
     * Light the ghost pipe is drawn with: full block and sky light, so it shows the same in the dark.
     */
    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * The ghost pipe's color: white, at the 0x50 alpha LP1 forced on it.
     */
    private static final int GHOST_COLOR = 0x50FFFFFF;

    /**
     * Back-face-culled translucent entity render type for the ghost pipe.
     *
     * <p>1.21.3 removed {@code RenderType.entityTranslucentCull}. Its surviving sibling
     * {@code entityTranslucent} is <em>not</em> a drop-in replacement: the only difference between
     * the two was {@code setCullState(NO_CULL)}, so switching to it draws the tube's back faces
     * through its front faces and the semi-transparent preview comes out blotchy. This rebuilds the
     * culled variant, identical to vanilla's {@code entity_translucent} minus the NO_CULL shard.
     * The dedicated {@code entity_translucent_cull} shader program is gone too, but culling is GL
     * state rather than anything the program did, so the plain translucent one is the same shader.</p>
     */
    private static final Function<Identifier, RenderType> GHOST_PIPE_RENDER_TYPE = Util.memoize(
        texture -> RenderType.create(
            "lp_entity_translucent_cull",
            // 1.21.11 folded RenderStateShard and CompositeState into RenderSetup: the shards are
            // now named builder steps, and the two booleans that used to be
            // positional arguments of create() moved in here as well.
            RenderSetup.builder(LPRenderTypes.GHOST_ENTITY_PIPELINE)
                .affectsCrumbling()
                .sortOnUpload()
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()));

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void renderTick(RenderFrameEvent.Pre event) {
        ClientViewController.instance().tick();
    }

    /**
     * The slot-finder overlay draws on top of an open container screen, so it runs on the screen's
     * own render event: that is the one that carries the graphics to draw into.
     */
    @SubscribeEvent
    public void screenRender(ScreenEvent.Render.Post event) {
        if (GuiOverlay.getInstance().isCompatibleGui()) {
            GuiOverlay.getInstance().renderOverGui(event.getGuiGraphics());
        }
        QuickSortMarkerOverlay.render(event.getGuiGraphics());
    }

    /**
     * The overlay takes the click that picks a slot, so the screen must not also see it: the player
     * is naming a slot, not moving what is in it. Pre is the only event that can still stop that.
     */
    @SubscribeEvent
    public void screenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (GuiOverlay.getInstance().isCompatibleGui() && GuiOverlay.getInstance().handleClick(event.getButton())) {
            event.setCanceled(true);
        }
    }

    /**
     * Draws the HUD targeting cross right after the vanilla crosshair, as LP1 did. The crosshair
     * layer's Post event only fires when the crosshair was drawn, which is the check LP1 made.
     */
    @SubscribeEvent
    public void renderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            LogisticsHUDRenderer.instance().renderPlayerDisplay(event.getGuiGraphics());
        }
    }

    /**
     * The ghost pipe is drawn through the feature dispatcher from a storage of its own.
     */
    private final SubmitNodeStorage ghostStorage = new SubmitNodeStorage();

    /**
     * 26.2 removed {@code MultiBufferSource}: geometry is submitted to a storage and a
     * {@code FeatureRenderDispatcher} frame draws it. The level renderer holds the dispatcher's one
     * frame open across every stage inside the level, so drawing our own storages has to wait for
     * {@code AfterLevel}. By then the level renderer has popped the camera's model-view matrix,
     * which is put back for the duration.
     */
    @SubscribeEvent
    public void renderWorldLast(RenderLevelStageEvent.AfterLevel event) {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(event.getModelViewMatrix());
        try {
            renderWorldLast(event.getPoseStack());
        } finally {
            modelViewStack.popMatrix();
        }
    }

    private void renderWorldLast(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        LogisticsHUDRenderer.instance().renderWorldRelative(gameTime, partialTick, poseStack, FULL_BRIGHT);

        Player player = mc.player;
        CoreUnroutedPipe pipe = heldPipe(player);
        if (player == null || pipe == null || !(mc.hitResult instanceof BlockHitResult blockHit)
            || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Level level = player.level();
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            pos = pos.relative(blockHit.getDirection());
        }

        ITubeOrientation orientation = null;
        if (pipe instanceof CoreMultiBlockPipe multiPipe) {
            orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());
            if (orientation == null
                || !multiPipe.fitsAt(level, pos.offset(orientation.getOffset()), orientation, target -> true)) {
                return;
            }
        } else if (!level.isEmptyBlock(pos)) {
            return;
        }

        // LP1 drew the pipe highlight model at the target position, with the plain white pipe
        // texture, see-through.
        poseStack.pushPose();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        Vec3i offset = orientation != null ? orientation.getOffset() : Vec3i.ZERO;
        poseStack.translate(
            pos.getX() + offset.getX() - cam.x + 0.001,
            pos.getY() + offset.getY() - cam.y + 0.001,
            pos.getZ() + offset.getZ() - cam.z + 0.001);

        // The tube preview tracks the player's facing. The removed legacy branch reached the
        // same geometry through rotations that CCLProxy converted from radians a second time, so
        // every orientation of a tube came out nearly identical — a curve did not appear to swing
        // left or right, and the S-curve, which is the gain model rolled a quarter turn about Z,
        // looked exactly like a gain.
        ObjMesh tube = TubeMeshes.forPipe(pipe, orientation).mesh();
        ObjMesh ghost = tube.isEmpty() ? PipeModelStore.parts().highlight() : tube;
        ghostStorage.submitCustomGeometry(poseStack, GHOST_PIPE_RENDER_TYPE.apply(GHOST_PIPE_TEXTURE),
            (pose, ghostBuffer) -> MeshRenderer.emitRaw(ghostBuffer, pose, ghost, GHOST_COLOR, FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY));
        LPRenderTypes.drawNow(ghostStorage);
        poseStack.popPose();
    }

    /**
     * The pipe the player holds in the main hand, to preview where it would go.
     *
     * @return the pipe, or null when there is none to preview: no player, no pipe in hand, or the
     * Request Table, which places as a full solid block and not as a pipe frame
     */
    private static @Nullable CoreUnroutedPipe heldPipe(@Nullable Player player) {
        if (player == null || !(player.getMainHandItem().getItem() instanceof ItemLogisticsPipe pipeItem)) {
            return null;
        }
        CoreUnroutedPipe pipe = pipeItem.getDummyPipe();
        return pipe instanceof PipeBlockRequestTable ? null : pipe;
    }
}
