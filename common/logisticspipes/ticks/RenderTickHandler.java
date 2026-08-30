package logisticspipes.ticks;

import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.tube.TubeMeshes;
import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.renderer.GuiOverlay;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.util.DoubleCoordinatesType;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.world.item.ItemLogisticsPipe;

public class RenderTickHandler {

	private static final Identifier GHOST_PIPE_TEXTURE = LPConstants.rl("textures/blocks/pipes/white.png");

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
			// now named builder steps, and the buffer size and the two booleans that used to be
			// positional arguments of create() moved in here as well.
			RenderSetup.builder(LPRenderTypes.GHOST_ENTITY_PIPELINE)
				.bufferSize(1536)
				.affectsCrumbling()
				.sortOnUpload()
				.withTexture("Sampler0", texture)
				.useLightmap()
				.useOverlay()
				.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
				.createRenderSetup()));

	private long renderTicks = 0;

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void renderTick(RenderFrameEvent.Pre event) {
		if (GuiOverlay.getInstance().isCompatibleGui()) {
			GuiOverlay.getInstance().preRender();
		}
		ClientViewController.instance().tick();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void renderTick(RenderFrameEvent.Post event) {
		renderTicks++;
		// TODO: migrate HUD rendering to 1.20 PoseStack / GameRenderer approach.
		// mc.entityRenderer.setupCameraTransform() and ActiveRenderInfo.updateRenderInfo() were removed.
		// See Task #7 (GameRenderer.setupCamera AT entry).
	}

	/** The slot-finder overlay draws on top of an open container screen, so it runs on the screen's own
	 *  render event: that is the one that carries the {@link net.minecraft.client.gui.GuiGraphicsExtractor} to draw
	 *  into. RenderFrameEvent.Post, where this used to live, provides none. */
	@SubscribeEvent
	public void screenRender(ScreenEvent.Render.Post event) {
		if (GuiOverlay.getInstance().isCompatibleGui()) {
			GuiOverlay.getInstance().renderOverGui(event.getGuiGraphics());
		}
	}

	/** LP1 drew the HUD targeting cross right after the vanilla crosshair; the CROSSHAIR
	 *  overlay Post event only fires when the crosshair actually rendered, matching
	 *  LP1's {@code GuiIngameForge.renderCrosshairs} check. */
	@SubscribeEvent
	public void renderGuiLayer(RenderGuiLayerEvent.Post event) {
		if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
			return;
		}

		LogisticsHUDRenderer.instance().renderPlayerDisplay(
				renderTicks,
				event.getGuiGraphics()
		);
	}

	@SubscribeEvent
	public void renderWorldLast(RenderLevelStageEvent.AfterTranslucentParticles worldEvent) {
		PoseStack poseStack = worldEvent.getPoseStack();
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		int packedLight = 0xF000F0;

		long renderTicks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
		LogisticsHUDRenderer.instance().renderWorldRelative(renderTicks, partialTick, poseStack, bufferSource, packedLight);
		bufferSource.endBatch();

		// We are not holding an Item that needs to render a ghost pipe!
		if (!displayPipeGhost()) {
            return;
        }

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		HitResult box = mc.hitResult;

		// The box is null or we are targeting something else than a block!
		if (box == null || box.getType() != HitResult.Type.BLOCK) return;

		BlockHitResult blockHit = (BlockHitResult) box;
		Inventory inventory = mc.player.getInventory();
		ItemStack stack = inventory.getItem(inventory.getSelectedSlot());
		CoreUnroutedPipe pipe = ((ItemLogisticsPipe) stack.getItem()).getDummyPipe();
		Level level = player.level();
		Direction side = blockHit.getDirection();
		BlockPos pos = blockHit.getBlockPos();
		Block block = level.getBlockState(pos).getBlock();

		if (block == Blocks.SNOW && level.getBlockState(pos).canBeReplaced()) {
			side = Direction.UP;
		} else if (!level.getBlockState(pos).canBeReplaced()) {
			pos = pos.relative(side);
		}

		boolean isFreeSpace = true;
		ITubeOrientation orientation = null;

		if (pipe instanceof CoreMultiBlockPipe multiPipe) {
            DoubleCoordinates placeAt = new DoubleCoordinates(pos);
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> globalPos = new LPPositionSet<>(DoubleCoordinatesType.class);
			globalPos.add(new DoubleCoordinatesType<>(placeAt, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = multiPipe.getSubBlocks();
			orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());

			if (orientation == null) return;

			orientation.rotatePositions(positions);
			positions.stream().map(p -> p.add(placeAt)).forEach(globalPos::add);
			globalPos.addToAll(orientation.getOffset());

			for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> posType : globalPos) {
				if (!level.isEmptyBlock(posType.getBlockPos())) {
					BlockEntity tile = level.getBlockEntity(posType.getBlockPos());
					boolean canPlace = false;
					if (tile instanceof LogisticsTileGenericSubMultiBlock) {
						if (CoreMultiBlockPipe.canShare(((LogisticsTileGenericSubMultiBlock) tile).getSubTypes(), posType.getType())) {
							canPlace = true;
						}
					}
					if (!canPlace) {
						isFreeSpace = false;
						break;
					}
				}
			}
		} else {
			if (!level.isEmptyBlock(pos)) {
				isFreeSpace = false;
			}
		}

		// No free space to render anything!
		if (!isFreeSpace) return;

		// Ghost pipe rendering — LP1 drew the pipe highlight model at the target position
		// with the plain white pipe texture and alpha forced to 0x50.
		poseStack.pushPose();
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		double gx = pos.getX() + (orientation != null ? orientation.getOffset().getXInt() : 0);
		double gy = pos.getY() + (orientation != null ? orientation.getOffset().getYInt() : 0);
		double gz = pos.getZ() + (orientation != null ? orientation.getOffset().getZInt() : 0);
		poseStack.translate(gx - cam.x + 0.001, gy - cam.y + 0.001, gz - cam.z + 0.001);

		VertexConsumer ghostBuffer = bufferSource.getBuffer(GHOST_PIPE_RENDER_TYPE.apply(GHOST_PIPE_TEXTURE));
		// The tube preview tracks the player's facing. The removed legacy branch reached the
		// same geometry through rotations that CCLProxy converted from radians a second time, so
		// every orientation of a tube came out nearly identical — a curve did not appear to swing
		// left or right, and the S-curve, which is the gain model rolled a quarter turn about Z,
		// looked exactly like a gain.
		ObjMesh ghost = TubeMeshes.forPipe(pipe, orientation).mesh();
		if (ghost.isEmpty()) {
			ghost = PipeModelStore.parts().highlight();
		}
		// 0x50 alpha, matching the alpha override the shared render state used to apply.
		MeshRenderer.emitRaw(ghostBuffer, poseStack.last(),
            ghost, 0x50FFFFFF, packedLight, OverlayTexture.NO_OVERLAY);
		bufferSource.endBatch();
		poseStack.popPose();
	}

	private boolean displayPipeGhost() {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
            return false;
        }

		Inventory pInventory = player.getInventory();

        NonNullList<ItemStack> inv = pInventory.getNonEquipmentItems();

        if (inv.size() <= pInventory.getSelectedSlot()
				|| !(inv.get(pInventory.getSelectedSlot()).getItem() instanceof ItemLogisticsPipe pipeItem)) {
			return false;
		}

		// The Request Table places as a full solid block, not as a pipe frame, so the pipe ghost
		// would preview geometry that has nothing to do with what gets placed.
		return !(pipeItem.getDummyPipe() instanceof PipeBlockRequestTable);
	}
}
