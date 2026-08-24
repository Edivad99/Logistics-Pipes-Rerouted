package logisticspipes.pipefxhandlers;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.client.particle.GlowGeometryParticle;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class PipeFXLaserPowerBall extends GlowGeometryParticle {

	private final float r;
	private final float g;
	private final float b;

	public PipeFXLaserPowerBall(ClientLevel level, DoubleCoordinates pos, int color, BlockEntity tile) {
		super(level, pos.getXCoord() + 0.5D, pos.getYCoord() + 0.5D, pos.getZCoord() + 0.5D);
		this.r = ((color >> 16) & 0xFF) / 255.0f;
		this.g = ((color >> 8) & 0xFF) / 255.0f;
		this.b = (color & 0xFF) / 255.0f;
		this.lifetime = 6000;
		this.hasPhysics = false;
		this.bbWidth = 0.3f;
		this.bbHeight = 0.3f;
	}

	@Override
	public void emit(VertexConsumer bb, Camera camera, float partialTicks) {
		double px = Mth.lerp(partialTicks, xo, x) - camera.position().x;
		double py = Mth.lerp(partialTicks, yo, y) - camera.position().y;
		double pz = Mth.lerp(partialTicks, zo, z) - camera.position().z;

		int ri = (int) (r * 255);
		int gi = (int) (g * 255);
		int bi = (int) (b * 255);
		int ai = 200;

		org.joml.Quaternionf rot = camera.rotation();
		org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0);
		org.joml.Vector3f up    = new org.joml.Vector3f(0, 1, 0);
		rot.transform(right);
		rot.transform(up);

		float s = this.bbWidth * 0.5f;

		// Two crossed billboard quads for a glowing ball look
		billboardVertex(bb, px, py, pz, right, up,  s,  s, ri, gi, bi, ai);
		billboardVertex(bb, px, py, pz, right, up, -s,  s, ri, gi, bi, ai);
		billboardVertex(bb, px, py, pz, right, up, -s, -s, ri, gi, bi, ai);
		billboardVertex(bb, px, py, pz, right, up,  s, -s, ri, gi, bi, ai);
	}

	private static void billboardVertex(VertexConsumer bb, double cx, double cy, double cz,
			org.joml.Vector3f right, org.joml.Vector3f up, float rs, float us,
			int r, int g, int b, int a) {
		bb.addVertex((float) (cx + right.x * rs + up.x * us),
				  (float) (cy + right.y * rs + up.y * us),
				  (float) (cz + right.z * rs + up.z * us))
		  .setColor(r, g, b, a);
	}
}
