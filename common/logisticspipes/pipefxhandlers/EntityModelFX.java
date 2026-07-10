package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.IRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class EntityModelFX extends Particle {

	private final IModel3D model;
	private final I3DOperation[] operations;
	private final ResourceLocation texture;

	public EntityModelFX(ClientLevel world, double x, double y, double z, IModel3D model, I3DOperation[] i3dOperations, ResourceLocation texture) {
		super(world, x, y, z, 0, -5, 0);
		this.model = model;
		this.operations = i3dOperations;
		this.texture = texture;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void render(VertexConsumer ignored, Camera camera, float partialTicks) {
		double dx = Mth.lerp(partialTicks, xo, x) - camera.getPosition().x;
		double dy = Mth.lerp(partialTicks, yo, y) - camera.getPosition().y;
		double dz = Mth.lerp(partialTicks, zo, z) - camera.getPosition().z;

		PoseStack poseStack = new PoseStack();
		poseStack.translate(dx, dy, dz);

		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		RenderType renderType = RenderType.entityTranslucentCull(texture);
		VertexConsumer buffer = bufferSource.getBuffer(renderType);

		BlockPos blockPos = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
		int packedLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, blockPos);

		IRenderState state = SimpleServiceLocator.cclProxy.getRenderState();
		state.reset();
		state.setAlphaOverride(0xff);
		if (state instanceof LPRenderStateImpl lpState) {
			lpState.bind(buffer, poseStack.last().pose(), poseStack.last().normal(), packedLight, OverlayTexture.NO_OVERLAY);
		}

		model.render(operations);

		bufferSource.endBatch(renderType);
	}

}
