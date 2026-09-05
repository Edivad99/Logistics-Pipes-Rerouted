package logisticspipes.utils.gui.sideconfig;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.client.renderer.ImmediateSubmitCollector;
import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.client.renderer.pip.SideConfigSceneState;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.util.CoordinateUtils;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.utils.math.BoundingBox;
import logisticspipes.utils.math.Camera;
import logisticspipes.utils.math.Matrix4d;
import logisticspipes.utils.math.Vector3d;
import logisticspipes.utils.math.Vertex;

public abstract class SideConfigDisplay {

	private static final float FOV = 30.0f;
	private static final float Z_NEAR = 0.05f;
	private static final float Z_FAR = 50.0f;

	/** Owns the GPU-side projection uniform this display uploads its own matrices through. */
	private final ProjectionMatrixBuffer projectionBuffer =
			new ProjectionMatrixBuffer("logisticspipes:side_config");

	/**
	 * The perspective the scene is drawn with, replacing the orthographic one the picture-in-picture
	 * machinery sets up for flat GUI content. It is the same frustum the camera picks rays against,
	 * so what the player clicks is what they see.
	 */
	private final Projection sceneProjection = new Projection();

	private boolean draggingRotate = false;
	private boolean draggingMove = false;
	private float pitch;
	private float yaw;
	private double distance;
	private long initTime;

	private Minecraft mc = Minecraft.getInstance();
	/**
	 * 26.1.2 moved {@code BlockAndTintGetter} into the client renderer package, and {@link Level}
	 * no longer implements it -- only {@link ClientLevel} does. This display is client-only and its
	 * level always comes from the player, so it holds the client type directly rather than casting
	 * at the one call site that needs it.
	 */
	private ClientLevel level;

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

		level = mc.level;
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
	public void onMouseClicked(int screenMouseX, int screenMouseY, Rectangle sceneRect) {
		if (!camera.isValid()) return;
		// Convert screen pixel to ray in camera space, then fire updateSelection. The ray is built
		// in clip space, where y points up, while a mouse position grows downward: without the flip
		// a click picks the face opposite the one under the cursor.
		int relX = screenMouseX - sceneRect.x;
		int relY = sceneRect.height - (screenMouseY - sceneRect.y);
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

	/**
	 * Hands the scene to the GUI renderer, which draws it during submission.
	 *
	 * @param sceneRect where the scene goes, in GUI coordinates
	 */
	public void submit(GuiGraphicsExtractor guiGraphics, Rectangle sceneRect) {
		if (!updateCamera(sceneRect.width, sceneRect.height)) {
			return;
		}
		guiGraphics.submitPictureInPictureRenderState(new SideConfigSceneState(this,
				sceneRect.x, sceneRect.y, sceneRect.x + sceneRect.width, sceneRect.y + sceneRect.height,
				guiGraphics.peekScissorStack()));
	}

	/**
	 * Draws the scene into the texture the picture-in-picture renderer set up.
	 *
	 * <p>The projection is swapped for the camera's perspective -- picture in picture hands over an
	 * orthographic one, meant for flat content -- and the camera's view matrix becomes the pose
	 * everything is submitted under, there being no modelview stack to push it onto since 1.21.6.
	 * The rectangle needs no scissor and the depth needs no clearing: the texture is both.
	 */
	public void renderToTexture(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
			int width, int height) {
		sceneProjection.setupPerspective(Z_NEAR, Z_FAR, FOV, width, height);
		RenderSystem.setProjectionMatrix(
				projectionBuffer.getBuffer(sceneProjection),
				ProjectionType.PERSPECTIVE
		);
		poseStack.setIdentity();
		poseStack.mulPose(toJoml(camera.getViewMatrix()));

		renderScene(poseStack, bufferSource);
		renderSelection(poseStack, bufferSource);
	}

	private void renderSelection(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		// The highlight sprite is one of the textures still waiting to be registered on 26.1, so the
		// face a click selected simply goes unmarked rather than taking the scene down with it.
		if (selection == null || !(Textures.LOGISTICS_SIDE_SELECTION instanceof TextureAtlasSprite icon)) {
			return;
		}
		BoundingBox bb = new BoundingBox(new DoubleCoordinates(selection.config));
		List<Vertex> corners = bb.getCornersWithUvForFace(selection.face, icon.getU0(), icon.getU1(), icon.getV0(), icon.getV1());

		// The block atlas, the translucent blend and the disabled depth test are all carried by
		// the render type. 1.21.6 removed every RenderType.gui* factory, so what used to be
		// guiTexturedOverlay is now LP's own equivalent -- same POSITION_TEX_COLOR format with the
		// depth test off, so the highlight still paints over the blocks behind it.
		RenderType renderType = LPRenderTypes.TEXTURED_OVERLAY.apply(RenderUtil.BLOCK_TEX);
		VertexConsumer buf = bufferSource.getBuffer(renderType);
		for (Vertex v : corners) {
			buf.addVertex(poseStack.last(), (float) (v.x() - origin.x), (float) (v.y() - origin.y),
					(float) (v.z() - origin.z))
				.setUv(v.u(), v.v())
				.setColor(0xFFFFFFFF);
		}
		bufferSource.endBatch(renderType);
	}

	/** Reused across blocks and frames, the way an entity render state is. */
	private final BlockModelRenderState renderState = new BlockModelRenderState();
	private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	private void renderScene(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		BlockModelResolver blockModels = Minecraft.getInstance().getBlockModelResolver();
		ImmediateSubmitCollector collector = new ImmediateSubmitCollector(bufferSource);

		for (DoubleCoordinates coord : configurables) {
			renderBlockAt(coord, blockModels, collector, poseStack, false);
		}
		if (renderNeighbours) {
			for (DoubleCoordinates coord : neighbours) {
				renderBlockAt(coord, blockModels, collector, poseStack, true);
			}
		}
		bufferSource.endBatch();
	}

	private void renderBlockAt(DoubleCoordinates coord, BlockModelResolver blockModels,
			ImmediateSubmitCollector collector, PoseStack poseStack, boolean transparent) {
		BlockPos pos = new BlockPos(coord.getXInt(), coord.getYInt(), coord.getZInt());
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return;
		poseStack.pushPose();
		poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
		// 26.1.2 removed BlockRenderDispatcher. A block drawn outside a chunk is resolved into a
		// BlockModelRenderState -- the same object an entity renderer holds for a carried block --
		// and submitted; ImmediateSubmitCollector puts the quads straight into our buffer source,
		// since the level renderer's collector is not reachable from a GUI.
		//
		// Lighting is full-bright rather than sampled from the level. The old path took the light
		// from the block's own position, which for a preview floating in a GUI was arbitrary
		// anyway, and there is no dispatcher left to ask.
		try {
			renderState.clear();
			blockModels.update(renderState, state, BLOCK_DISPLAY_CONTEXT);
			renderState.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		} catch (Exception ignored) {}
		poseStack.popPose();
	}

	private boolean updateCamera(int width, int height) {
		if (width <= 0 || height <= 0) {
			return false;
		}
		// The viewport is the scene rectangle itself, so a click maps to a ray without knowing
		// where on the screen the rectangle sits or how large a GUI pixel currently is.
		camera.setViewport(0, 0, width, height);
		camera.setProjectionMatrixAsPerspective(FOV, Z_NEAR, Z_FAR, width, height);
		eye.set(0, 0, distance);
		pitchRot.makeRotationX(Math.toRadians(pitch));
		yawRot.makeRotationY(Math.toRadians(yaw));
		pitchRot.transform(eye);
		yawRot.transform(eye);
		camera.setViewMatrixAsLookAt(eye, RenderUtil.ZERO_V, RenderUtil.UP_V);
		return camera.isValid();
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
		public static final Identifier BLOCK_TEX = TextureAtlas.LOCATION_BLOCKS;
	}
}
