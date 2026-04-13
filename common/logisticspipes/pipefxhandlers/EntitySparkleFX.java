package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.Random;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;



public class EntitySparkleFX extends Particle {

	public int multiplier;
	public boolean shrink;
	public int particle;
	public int blendmode;

	public EntitySparkleFX(ClientLevel world, double x, double y, double z, float scalemult, float red, float green, float blue, int var12) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		shrink = false;
		particle = 0;
		blendmode = 1;

		rCol = red;
		gCol = green;
		bCol = blue;
		gravity = 0.07F;
		xd = 0.0D;
		yd = 0.0D;
		zd = 0.0D;
		this.scale(scalemult);
		lifetime = 3 * var12 - 1;
		multiplier = var12;
		hasPhysics = false;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void render(VertexConsumer ignored, Camera camera, float partialTicks) {
		double px = Mth.lerp(partialTicks, xo, x) - camera.getPosition().x;
		double py = Mth.lerp(partialTicks, yo, y) - camera.getPosition().y;
		double pz = Mth.lerp(partialTicks, zo, z) - camera.getPosition().z;

		float ageRatio = lifetime > 0 ? (float) age / lifetime : 1.0f;
		float alpha = 0.75f * (1.0f - ageRatio);
		if (alpha <= 0.01f) return;

		// Build a camera-facing (billboard) quad
		org.joml.Quaternionf rot = camera.rotation();
		org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0);
		org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0);
		rot.transform(right);
		rot.transform(up);

		float s = this.bbWidth * 0.5f;

		int r = (int) (rCol * 255);
		int g = (int) (gCol * 255);
		int b = (int) (bCol * 255);
		int a = (int) (alpha * 255);

		Tesselator tes = Tesselator.getInstance();
		BufferBuilder bb = tes.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);

		bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		billboardVertex(bb, px, py, pz, right, up, s, s, r, g, b, a);
		billboardVertex(bb, px, py, pz, right, up, -s, s, r, g, b, a);
		billboardVertex(bb, px, py, pz, right, up, -s, -s, r, g, b, a);
		billboardVertex(bb, px, py, pz, right, up, s, -s, r, g, b, a);
		BufferUploader.drawWithShader(bb.end());

		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	private static void billboardVertex(BufferBuilder bb, double cx, double cy, double cz,
			org.joml.Vector3f right, org.joml.Vector3f up, float rs, float us,
			int r, int g, int b, int a) {
		bb.vertex((float) (cx + right.x * rs + up.x * us),
				  (float) (cy + right.y * rs + up.y * us),
				  (float) (cz + right.z * rs + up.z * us))
		  .color(r, g, b, a).endVertex();
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void tick() {
		try {
			LocalPlayer var1 = Minecraft.getInstance().player;

			if (var1.distanceToSqr(x, y, z) > 2500) {
				remove();
			}

			xo = x;
			yo = y;
			zo = z;

			if (age++ >= lifetime) {
				remove();
			}

			xd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();
			yd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();
			zd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();

			move(xd, yd, zd);
			xd *= 0.9800000190734863D;
			yd *= 0.9800000190734863D;
			zd *= 0.9800000190734863D;

			if (onGround) {
				xd *= 0.699999988079071D;
				zd *= 0.699999988079071D;
			}
		} catch (Exception ignored) {
		}
	}
}
