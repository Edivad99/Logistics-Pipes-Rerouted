package logisticspipes.utils.gui.sideconfig;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.client.renderer.pip.SideConfigSceneState;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.utils.Color;

public abstract class SideConfigDisplay {

	private static final float FOV = 30.0f;
	private static final float Z_NEAR = 0.05f;
	private static final float Z_FAR = 50.0f;

    private static final int HIGHLIGHT_TINT = Color.getValue(Color.RED);

	private static final Vector3dc SCENE_CENTER = new Vector3d(0, 0, 0);
	private static final Vector3dc SCENE_UP = new Vector3d(0, 1, 0);

	/**
	 * Stops the orbit a hair short of the pole. At exactly +-90 the eye sits on {@link #SCENE_UP},
	 * leaving the look-at basis undefined and every matrix NaN. The hand-rolled math this replaced
	 * only escaped that because {@code Math.cos} of a right angle is not quite zero.
	 */
	private static final float MAX_PITCH = 89.99f;

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
	private final @Nullable ClientLevel level;

	private final Vector3d origin = new Vector3d();
	private final Vector3d eye = new Vector3d();
	private final Matrix4d viewMatrix = new Matrix4d();
	private final Matrix4d projectionMatrix = new Matrix4d();
	private int viewportWidth;
	private int viewportHeight;
	/** Both matrices and the viewport are set together by {@link #updateCamera}, or not at all. */
	private boolean cameraValid;

	public BlockPos originBC;

	private List<BlockPos> configurables = new ArrayList<>();
	private List<BlockPos> neighbours = new ArrayList<>();

	private @Nullable SelectedFace selection;

	/** Faces already configured when the popup opened, drawn alongside whatever a click picks. */
	private final List<HighlightedFace> highlights = new ArrayList<>();

	public boolean renderNeighbours = true;

	public SideConfigDisplay(CoreRoutedPipe configurables) {
		this(Collections.singletonList(configurables.getPos()));
	}

	public SideConfigDisplay(List<BlockPos> configurables) {
		this.configurables.addAll(configurables);

		Vector3d c;
		Vector3d size;
		if (configurables.size() == 1) {
			BlockPos bc = this.configurables.get(0);
			c = new Vector3d(bc.getX() + 0.5, bc.getY() + 0.5, bc.getZ() + 0.5);
			size = new Vector3d(1, 1, 1);
		} else {
			Vector3d min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
			Vector3d max = new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
			for (BlockPos bc : configurables) {
				min.set(Math.min(bc.getX(), min.x), Math.min(bc.getY(), min.y), Math
						.min(bc.getZ(), min.z));
				max.set(Math.max(bc.getX(), max.x), Math.max(bc.getY(), max.y), Math
						.max(bc.getZ(), max.z));
			}
			size = new Vector3d(max).sub(min).mul(0.5);
			c = new Vector3d(min.x + size.x, min.y + size.y, min.z + size.z);
			size.mul(2);
		}

		originBC = BlockPos.containing(c.x, c.y, c.z);
		origin.set(c);

		pitch = Math.clamp(-mc.player.getXRot(), -MAX_PITCH, MAX_PITCH);
		yaw = 180 - mc.player.getYRot();

		distance = Math.max(Math.max(size.x, size.y), size.z) + 4;

		for (BlockPos bc : configurables) {
			for (Direction dir : Direction.values()) {
				BlockPos loc = bc.relative(dir);
				if (!configurables.contains(loc)) {
					neighbours.add(loc);
				}
			}
		}

		level = mc.level;
	}

	public abstract void handleSelection(SelectedFace selection);

	/** A face the selection marker is painted on. */
	public record HighlightedFace(BlockPos pos, Direction face) {}

	/**
	 * Marks a face as already configured, so opening the popup shows what the upgrade is set to
	 * rather than an empty scene. Independent of {@link #getSelection}, which is what a click picks.
	 */
	public void highlight(BlockPos pos, Direction face) {
		highlights.add(new HighlightedFace(pos, face));
	}

	public void init() {
		initTime = System.currentTimeMillis();
	}

	@Nullable
	public SelectedFace getSelection() {
		return selection;
	}

	/** Called by the parent Screen's mouseDragged; rotates the camera. */
	public void onMouseDragged(double dx, double dy, int button) {
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			yaw += (float) dx;
			pitch += (float) dy;
			pitch = Math.clamp(pitch, -MAX_PITCH, MAX_PITCH);
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

		for (BlockPos coord : configurables) {
			BlockPos pos = new BlockPos(coord.getX(), coord.getY(), coord.getZ());
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

	@Nullable
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
		if (!cameraValid) return;
		// Convert screen pixel to ray in camera space, then fire updateSelection. The ray is built
		// in clip space, where y points up, while a mouse position grows downward: without the flip
		// a click picks the face opposite the one under the cursor.
		int relX = screenMouseX - sceneRect.x;
		int relY = sceneRect.height - (screenMouseY - sceneRect.y);

		Matrix4d inverseView = viewMatrix.invert(new Matrix4d());
		Matrix4d clipToWorld = inverseView.mul(projectionMatrix.invert(new Matrix4d()), new Matrix4d());
		Vector3d rayEye = inverseView.getTranslation(new Vector3d());

		double clipX = (double) relX / viewportWidth * 2.0 - 1.0;
		double clipY = (double) relY / viewportHeight * 2.0 - 1.0;
		Vector3d near = unproject(clipToWorld, clipX, clipY, -1.0);
		Vector3d far = unproject(clipToWorld, clipX, clipY, 1.0);

		Vector3d end = far.sub(near).normalize().mul(100).add(rayEye);
		updateSelection(rayEye, end);
		if (selection != null) {
			handleSelection(selection);
		}
	}

	/** The world point a clip-space position maps back to, perspective divide included. */
	private static Vector3d unproject(Matrix4d clipToWorld, double clipX, double clipY, double clipZ) {
		Vector4d p = clipToWorld.transform(new Vector4d(clipX, clipY, clipZ, 1.0));
		return new Vector3d(p.x / p.w, p.y / p.w, p.z / p.w);
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
	 *
	 * <p>Nothing is drawn here: the picture-in-picture renderer renders the collector once this
	 * returns, with the projection set below still in place.</p>
	 */
	public void renderToTexture(PoseStack poseStack, SubmitNodeCollector collector,
			int width, int height) {
		sceneProjection.setupPerspective(Z_NEAR, Z_FAR, FOV, width, height);
		RenderSystem.setProjectionMatrix(
				projectionBuffer.getBuffer(sceneProjection),
				ProjectionType.PERSPECTIVE
		);
		poseStack.setIdentity();
		poseStack.mulPose(new Matrix4f(viewMatrix));

		renderScene(poseStack, collector);
		renderSelection(poseStack, collector);
	}

	private void renderSelection(PoseStack poseStack, SubmitNodeCollector collector) {
		// TextureRegistrar binds the sprite during the block atlas stitch; before that has run, or
		// on a resource pack without it, the marked faces simply go unmarked.
		TextureAtlasSprite icon = TextureRegistrar.sideSelectionSprite();
		if (icon == null) {
			return;
		}
		if (highlights.isEmpty() && selection == null) {
			return;
		}

		// The block atlas, the translucent blend and the disabled depth test are all carried by
		// the render type. 1.21.6 removed every RenderType.gui* factory, so what used to be
		// guiTexturedOverlay is now LP's own equivalent -- same POSITION_TEX_COLOR format with the
		// depth test off, so the highlight still paints over the blocks behind it.
		RenderType renderType = LPRenderTypes.TEXTURED_OVERLAY.apply(RenderUtil.BLOCK_TEX);
		// Run when the collector is rendered, so the faces are copied out of the fields now.
		List<HighlightedFace> marked = new ArrayList<>(highlights);
		if (selection != null) {
			marked.add(new HighlightedFace(selection.config.getBlockPos(), selection.face));
		}
		collector.submitCustomGeometry(poseStack, renderType, (pose, buf) -> {
			for (HighlightedFace face : marked) {
				drawFace(buf, pose, face.pos(), face.face(), icon);
			}
		});
	}

	private void drawFace(VertexConsumer buf, PoseStack.Pose pose, BlockPos pos, Direction face,
			TextureAtlasSprite icon) {
		for (FaceCorner c : faceCorners(pos, face, icon.getU0(), icon.getU1(), icon.getV0(), icon.getV1())) {
			buf.addVertex(pose, (float) (c.x() - origin.x), (float) (c.y() - origin.y),
					(float) (c.z() - origin.z))
				.setUv(c.u(), c.v())
				.setColor(HIGHLIGHT_TINT);
		}
	}

	/** One corner of the highlighted face: a world position and the atlas UV that goes on it. */
	private record FaceCorner(float x, float y, float z, float u, float v) {}

	/**
	 * The four corners of one face of the block at {@code pos}, wound the way the overlay quad
	 * wants them. Only the highlight needs this, so there is no normal and no colour.
	 */
	private static List<FaceCorner> faceCorners(BlockPos pos, Direction face,
			float minU, float maxU, float minV, float maxV) {
		float minX = pos.getX();
		float minY = pos.getY();
		float minZ = pos.getZ();
		float maxX = minX + 1;
		float maxY = minY + 1;
		float maxZ = minZ + 1;
		return switch (face) {
			case NORTH -> List.of(
					new FaceCorner(maxX, minY, minZ, minU, minV),
					new FaceCorner(minX, minY, minZ, maxU, minV),
					new FaceCorner(minX, maxY, minZ, maxU, maxV),
					new FaceCorner(maxX, maxY, minZ, minU, maxV));
			case SOUTH -> List.of(
					new FaceCorner(minX, minY, maxZ, maxU, minV),
					new FaceCorner(maxX, minY, maxZ, minU, minV),
					new FaceCorner(maxX, maxY, maxZ, minU, maxV),
					new FaceCorner(minX, maxY, maxZ, maxU, maxV));
			case EAST -> List.of(
					new FaceCorner(maxX, maxY, minZ, maxU, maxV),
					new FaceCorner(maxX, maxY, maxZ, minU, maxV),
					new FaceCorner(maxX, minY, maxZ, minU, minV),
					new FaceCorner(maxX, minY, minZ, maxU, minV));
			case WEST -> List.of(
					new FaceCorner(minX, minY, minZ, maxU, minV),
					new FaceCorner(minX, minY, maxZ, minU, minV),
					new FaceCorner(minX, maxY, maxZ, minU, maxV),
					new FaceCorner(minX, maxY, minZ, maxU, maxV));
			case UP -> List.of(
					new FaceCorner(maxX, maxY, maxZ, minU, minV),
					new FaceCorner(maxX, maxY, minZ, minU, maxV),
					new FaceCorner(minX, maxY, minZ, maxU, maxV),
					new FaceCorner(minX, maxY, maxZ, maxU, minV));
			case DOWN -> List.of(
					new FaceCorner(minX, minY, minZ, maxU, maxV),
					new FaceCorner(maxX, minY, minZ, minU, maxV),
					new FaceCorner(maxX, minY, maxZ, minU, minV),
					new FaceCorner(minX, minY, maxZ, maxU, minV));
		};
	}

	/** Reused across blocks and frames, the way an entity render state is. */
	private final BlockModelRenderState renderState = new BlockModelRenderState();
	private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	private void renderScene(PoseStack poseStack, SubmitNodeCollector collector) {
		BlockModelResolver blockModels = Minecraft.getInstance().getBlockModelResolver();

		for (BlockPos coord : configurables) {
			renderBlockAt(coord, blockModels, collector, poseStack, false);
		}
		if (renderNeighbours) {
			for (BlockPos coord : neighbours) {
				renderBlockAt(coord, blockModels, collector, poseStack, true);
			}
		}
	}

	private void renderBlockAt(BlockPos coord, BlockModelResolver blockModels,
			SubmitNodeCollector collector, PoseStack poseStack, boolean transparent) {
		BlockPos pos = new BlockPos(coord.getX(), coord.getY(), coord.getZ());
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return;
		poseStack.pushPose();
		poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
		// 26.1.2 removed BlockRenderDispatcher. A block drawn outside a chunk is resolved into a
		// BlockModelRenderState -- the same object an entity renderer holds for a carried block --
		// and submitted to the picture-in-picture renderer's collector.
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
		viewportWidth = width;
		viewportHeight = height;
		projectionMatrix.setPerspective(Math.toRadians(FOV), (double) width / height, Z_NEAR, Z_FAR);
		eye.set(0, 0, distance)
				.rotateX(Math.toRadians(pitch))
				.rotateY(Math.toRadians(yaw));
		viewMatrix.setLookAt(eye, SCENE_CENTER, SCENE_UP);
		cameraValid = true;
		return true;
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

		public static final Identifier BLOCK_TEX = TextureAtlas.LOCATION_BLOCKS;
	}
}
