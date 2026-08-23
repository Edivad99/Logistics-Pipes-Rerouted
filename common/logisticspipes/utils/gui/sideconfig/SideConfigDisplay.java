package logisticspipes.utils.gui.sideconfig;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.utils.math.BoundingBox;
import logisticspipes.utils.math.Camera;
import logisticspipes.utils.math.Matrix4d;
import logisticspipes.utils.math.Vector3d;
import logisticspipes.utils.math.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import logisticspipes.client.renderer.LPRenderTypes;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.model.data.ModelData;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import com.mojang.blaze3d.ProjectionType;

//Based on: https://github.com/SleepyTrousers/EnderIO/blob/master/src/main/java/crazypants/enderio/machine/gui/GuiOverlayIoConfig.java
public abstract class SideConfigDisplay {

	/** Owns the GPU-side projection uniform this display uploads its own matrices through. */
	private final PerspectiveProjectionMatrixBuffer projectionBuffer =
			new PerspectiveProjectionMatrixBuffer("logisticspipes:side_config");

	private boolean draggingRotate = false;
	private boolean draggingMove = false;
	private float pitch;
	private float yaw;
	private double distance;
	private long initTime;

	private Minecraft mc = Minecraft.getInstance();
	private Level level;

	private final Vector3d origin = new Vector3d();
	private final Vector3d eye = new Vector3d();
	private final Camera camera = new Camera();
	private final Matrix4d pitchRot = new Matrix4d();
	private final Matrix4d yawRot = new Matrix4d();

	public DoubleCoordinates originBC;

	private List<DoubleCoordinates> configurables = new ArrayList<>();
	private List<DoubleCoordinates> neighbours = new ArrayList<>();

	private SelectedFace selection;

	public boolean renderNeighbours = true;

	public SideConfigDisplay(CoreRoutedPipe configurables) {
		this(Collections.singletonList(configurables.getLPPosition()));
	}

	public SideConfigDisplay(LPPositionSet<DoubleCoordinates> configurables) {
		this(Arrays.asList(configurables.toArray(new DoubleCoordinates[0])));
	}

	public SideConfigDisplay(List<DoubleCoordinates> configurables) {
		this.configurables.addAll(configurables);

		Vector3d c;
		Vector3d size;
		if (configurables.size() == 1) {
			DoubleCoordinates bc = this.configurables.get(0);
			c = new Vector3d(bc.getXDouble() + 0.5, bc.getYDouble() + 0.5, bc.getZDouble() + 0.5);
			size = new Vector3d(1, 1, 1);
		} else {
			Vector3d min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
			Vector3d max = new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
			for (DoubleCoordinates bc : configurables) {
				min.set(Math.min(bc.getXDouble(), min.x), Math.min(bc.getYDouble(), min.y), Math
						.min(bc.getZDouble(), min.z));
				max.set(Math.max(bc.getXDouble(), max.x), Math.max(bc.getYDouble(), max.y), Math
						.max(bc.getZDouble(), max.z));
			}
			size = new Vector3d(max);
			size.sub(min);
			size.multiply(0.5);
			c = new Vector3d(min.x + size.x, min.y + size.y, min.z + size.z);
			size.multiply(2);
		}

		originBC = new DoubleCoordinates((int) c.x, (int) c.y, (int) c.z);
		origin.set(c);
		pitchRot.setIdentity();
		yawRot.setIdentity();

		pitch = -mc.player.getXRot();
		yaw = 180 - mc.player.getYRot();

		distance = Math.max(Math.max(size.x, size.y), size.z) + 4;

		for (DoubleCoordinates bc : configurables) {
			for (Direction dir : Direction.values()) {
				DoubleCoordinates loc = CoordinateUtils.add(new DoubleCoordinates(bc), dir);
				if (!configurables.contains(loc)) {
					neighbours.add(loc);
				}
			}
		}

		level = mc.player.level();
	}

	public abstract void handleSelection(SelectedFace selection);

	public void init() {
		initTime = System.currentTimeMillis();
	}

	public SelectedFace getSelection() {
		return selection;
	}

	/** Called by the parent Screen's mouseDragged; rotates the camera. */
	public void onMouseDragged(double dx, double dy, int button) {
		if (button == 0) {
			yaw += (float) dx;
			pitch += (float) dy;
			pitch = Math.clamp(pitch, -90, 90);
		}
	}

	/** Called by the parent Screen's mouseScrolled; zooms in/out. */
	public void onMouseScrolled(double scrollY) {
		distance = Math.clamp(distance - scrollY, 1.5, 20.0);
	}

	private void updateSelection(Vector3d start, Vector3d end) {
		// Convert camera-relative ray to world coordinates
		Vec3 worldStart = new Vec3(origin.x + start.x, origin.y + start.y, origin.z + start.z);
		Vec3 worldEnd   = new Vec3(origin.x + end.x,   origin.y + end.y,   origin.z + end.z);

		selection = null;
		double minDist = Double.POSITIVE_INFINITY;

		for (DoubleCoordinates coord : configurables) {
			BlockPos pos = new BlockPos(coord.getXInt(), coord.getYInt(), coord.getZInt());
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos);
			if (shape.isEmpty()) continue;
			AABB box = shape.bounds().move(pos);
			box.clip(worldStart, worldEnd).ifPresent(pt -> {
				Direction face = Direction.getApproximateNearest(
					(float)(pt.x - (pos.getX() + 0.5)),
					(float)(pt.y - (pos.getY() + 0.5)),
					(float)(pt.z - (pos.getZ() + 0.5)));
				double d = pt.distanceToSqr(worldStart);
				if (d < minDist) {
					BlockEntity be = level.getBlockEntity(pos);
					if (be != null) {
						selection = new SelectedFace(be, face,
							new BlockHitResult(pt, face, pos, false));
					}
				}
			});
		}
	}


	public static HitResult getClosestHit(Vec3 origin, Collection<HitResult> candidates) {
		double minLengthSquared = Double.POSITIVE_INFINITY;
		HitResult closest = null;

		for (HitResult hit : candidates) {
			if (hit != null) {
				double lengthSquared = hit.getLocation().distanceToSqr(origin);
				if (lengthSquared < minLengthSquared) {
					minLengthSquared = lengthSquared;
					closest = hit;
				}
			}
		}
		return closest;
	}

	/** Called by the parent Screen on left-click; performs a ray cast and fires handleSelection if a face is hit. */
	public void onMouseClicked(int screenMouseX, int screenMouseY, Rectangle vp) {
		if (!camera.isValid()) return;
		// Convert screen pixel to ray in camera space, then fire updateSelection
		int relX = screenMouseX - vp.x / (int) mc.getWindow().getGuiScale();
		int relY = screenMouseY - vp.y / (int) mc.getWindow().getGuiScale();
		Vector3d rayEye = new Vector3d();
		Vector3d rayDir = new Vector3d();
		if (camera.getRayForPixel(relX, relY, rayEye, rayDir)) {
			Vector3d end = new Vector3d(rayDir);
			end.multiply(100);
			end.add(rayEye);
			updateSelection(rayEye, end);
			if (selection != null) {
				handleSelection(selection);
			}
		}
	}

	public void drawScreen(int par1, int par2, float partialTick, Rectangle vp, Rectangle parentBounds) {
		if (!updateCamera(partialTick, vp.x, vp.y, vp.width, vp.height)) {
			return;
		}
		applyCamera(partialTick);
		renderScene();
		renderSelection();
		renderOverlay(par1, par2);
	}

	private void renderSelection() {
		if (selection == null) {
			return;
		}
		BoundingBox bb = new BoundingBox(new DoubleCoordinates(selection.config));
		TextureAtlasSprite icon = (TextureAtlasSprite) Textures.LOGISTICS_SIDE_SELECTION;
		List<Vertex> corners = bb.getCornersWithUvForFace(selection.face, icon.getU0(), icon.getU1(), icon.getV0(), icon.getV1());

		// The block atlas, the translucent blend and the disabled depth test are all carried by
		// the render type. 1.21.6 removed every RenderType.gui* factory, so what used to be
		// guiTexturedOverlay is now LP's own equivalent -- same POSITION_TEX_COLOR format with the
		// depth test off, so the highlight still paints over the blocks behind it.
		RenderType renderType = LPRenderTypes.TEXTURED_OVERLAY.apply(RenderUtil.BLOCK_TEX);
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer buf = bufferSource.getBuffer(renderType);
		for (Vertex v : corners) {
			buf.addVertex((float) (v.x() - origin.x), (float) (v.y() - origin.y), (float) (v.z() - origin.z))
				.setUv(v.u(), v.v())
				.setColor(0xFFFFFFFF);
		}
		bufferSource.endBatch(renderType);
	}

	private void renderOverlay(int mx, int my) {
		// Restore modelview stack pushed in applyCamera
//		PoseStack modelViewStack = RenderSystem.getModelViewStack();
//		modelViewStack.popPose();
//		RenderSystem.applyModelViewMatrix();
//		// Restore projection matrix backed up in applyCamera
//		RenderSystem.restoreProjectionMatrix();
		// Restore the full-screen draw area. RenderSystem.viewport is gone since 1.21.5 -- the
		// viewport belongs to the RenderPass now -- so the scene rectangle is clipped with a
		// scissor instead, which is what has to be lifted here. 1.21.6 moved the generic scissor
		// state out of the pipeline code, leaving only the render-type-draw variant.
		RenderSystem.disableScissorForRenderTypeDraws();
		// restore projection vanilla
		// 1.21.6 uploads the projection through a uniform buffer: setProjectionMatrix takes a
		// GpuBufferSlice, which PerspectiveProjectionMatrixBuffer produces from a Matrix4f.
		RenderSystem.setProjectionMatrix(
				projectionBuffer.getBuffer(Minecraft.getInstance().gameRenderer.getProjectionMatrix(70.0f)),
				ProjectionType.PERSPECTIVE
		);
	}

	private void renderScene() {
		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		PoseStack poseStack = new PoseStack();

		for (DoubleCoordinates coord : configurables) {
			renderBlockAt(coord, blockRenderer, bufferSource, poseStack, false);
		}
		if (renderNeighbours) {
			for (DoubleCoordinates coord : neighbours) {
				renderBlockAt(coord, blockRenderer, bufferSource, poseStack, true);
			}
		}
		bufferSource.endBatch();
	}

	private void renderBlockAt(DoubleCoordinates coord, BlockRenderDispatcher blockRenderer,
			MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean transparent) {
		BlockPos pos = new BlockPos(coord.getXInt(), coord.getYInt(), coord.getZInt());
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return;
		poseStack.pushPose();
		poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
		// RenderType.translucent() went with the chunk-layer rework in 1.21.6; the supported way to
		// draw a block outside a chunk is getMovingBlockRenderType, which picks the layer the block
		// actually declares. That means the `transparent` neighbours no longer get a translucent
		// layer -- but they never actually looked translucent either, since the block textures are
		// opaque and the enableBlend that once wrapped this call had already become a no-op.
		RenderType renderType = ItemBlockRenderTypes.getMovingBlockRenderType(state);
		try {
			// 1.21.5 collects the geometry up front instead of letting the dispatcher pull it
			// from a model plus a RandomSource: renderBatched now takes the baked parts.
			List<BlockModelPart> parts = blockRenderer.getBlockModel(state)
				.collectParts(level, pos, state, level.getRandom());
			blockRenderer.renderBatched(state, pos, level, poseStack,
				bufferSource.getBuffer(renderType), false, parts);
		} catch (Exception ignored) {}
		poseStack.popPose();
	}

	private boolean updateCamera(float partialTick, int vpx, int vpy, int vpw, int vph) {
		if (vpw <= 0 || vph <= 0) {
			return false;
		}
		camera.setViewport(vpx, vpy, vpw, vph);
		camera.setProjectionMatrixAsPerspective(30, 0.05, 50, vpw, vph);
		eye.set(0, 0, distance);
		pitchRot.makeRotationX(Math.toRadians(pitch));
		yawRot.makeRotationY(Math.toRadians(yaw));
		pitchRot.transform(eye);
		yawRot.transform(eye);
		camera.setViewMatrixAsLookAt(eye, RenderUtil.ZERO_V, RenderUtil.UP_V);
		return camera.isValid();
	}

	private void applyCamera(float partialTick) {
		Rectangle vp = camera.getViewport();
		// Clip drawing to the 3D scene rectangle. RenderSystem.viewport no longer exists in
		// 1.21.5 (the viewport is a property of the RenderPass), so this is a scissor; the
		// projection matrix set below is what actually maps the scene into the rectangle.
		RenderSystem.enableScissorForRenderTypeDraws(vp.x, vp.y, vp.width, vp.height);
		// Clear the depth buffer so blocks render over the GUI background. GlStateManager is
		// no longer reachable from here either; clears go through the GPU command encoder.
		RenderSystem.getDevice().createCommandEncoder()
			.clearDepthTexture(Minecraft.getInstance().getMainRenderTarget().getDepthTexture(), 1.0);
		// Swap in custom perspective projection matrix
//		RenderSystem.backupProjectionMatrix();
//		RenderSystem.setProjectionMatrix(toJoml(camera.getProjectionMatrix()), ProjectionType.PERSPECTIVE);
//		// Load view matrix into the modelview stack
//		PoseStack modelViewStack = RenderSystem.getModelViewStack();
//		modelViewStack.pushPose();
//		modelViewStack.last().pose().set(toJoml(camera.getViewMatrix()));
//		RenderSystem.applyModelViewMatrix();

		RenderSystem.setProjectionMatrix(
				projectionBuffer.getBuffer(toJoml(camera.getProjectionMatrix())),
				ProjectionType.PERSPECTIVE
		);
	}

	/** Convert our row-major Matrix4d to a JOML column-major Matrix4f for RenderSystem. */
	private static org.joml.Matrix4f toJoml(Matrix4d m) {
		// JOML Matrix4f(m00,m01,...) fills column 0 rows 0-3, then column 1, etc.
		// Our Matrix4d.mRC is row R, col C; swap to get column-major layout.
		return new org.joml.Matrix4f(
			(float) m.m00, (float) m.m10, (float) m.m20, (float) m.m30,
			(float) m.m01, (float) m.m11, (float) m.m21, (float) m.m31,
			(float) m.m02, (float) m.m12, (float) m.m22, (float) m.m32,
			(float) m.m03, (float) m.m13, (float) m.m23, (float) m.m33
		);
	}

	public static class SelectedFace {

		public BlockEntity config;
		public Direction face;
		public HitResult hit;

		public SelectedFace(BlockEntity config, Direction face, HitResult hit) {
			super();
			this.config = config;
			this.face = face;
			this.hit = hit;
		}
	}

	/*
	private static class RenderPassHelper {
		private static Field worldRenderPass = null;
		private static int savedWorldRenderPass = -1;
		private static int savedEntityRenderPass = -1;

		static {
			try {
				worldRenderPass = ForgeHooksClient.class.getDeclaredField("worldRenderPass");
				worldRenderPass.setAccessible(true);
			} catch (Exception e) {
				LogisticsPipes.log.warn("Failed to access ForgeHooksClient.worldRenderPass because of: " + e);
				e.printStackTrace();
			}
		}

		public static void setBlockRenderPass(int pass) {
			savedWorldRenderPass = ForgeHooksClient.getWorldRenderPass();
			savedEntityRenderPass = MinecraftForgeClient.getRenderPass();
			setBlockRenderPassImpl(pass);
			setEntityRenderPass(pass);
		}

		private static void setBlockRenderPassImpl(int pass) {
			if (worldRenderPass != null) {
				try {
					worldRenderPass.setInt(null, pass);
				} catch (Exception e) {
					LogisticsPipes.log.warn("Failed to access ForgeHooksClient.worldRenderPass because of: " + e);
					e.printStackTrace();
					worldRenderPass = null;
				}
			}
		}

		private static void clearBlockRenderPass() {
			setBlockRenderPassImpl(savedWorldRenderPass);
			setEntityRenderPass(savedEntityRenderPass);
		}

		private static void clearEntityRenderPass() {
			ForgeHooksClient.setRenderPass(-1);
		}

		private static void setEntityRenderPass(int pass) {
			ForgeHooksClient.setRenderPass(pass);
		}
	}
*/
	private static class RenderUtil {

		public static final Vector3d UP_V = new Vector3d(0, 1, 0);
		public static final Vector3d ZERO_V = new Vector3d(0, 0, 0);
		public static final ResourceLocation BLOCK_TEX = TextureAtlas.LOCATION_BLOCKS;
	}
}
