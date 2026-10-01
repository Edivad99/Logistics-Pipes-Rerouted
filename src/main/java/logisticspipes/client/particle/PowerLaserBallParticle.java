package logisticspipes.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import logisticspipes.LPConstants;

/**
 * The glowing ball a power laser draws on its pipe: 27 textured quads turned 60 degrees apart about
 * all three axes, the whole of it spinning, as LP1 drew it.
 */
public class PowerLaserBallParticle extends GlowGeometryParticle {

    private static final Identifier TEXTURE = LPConstants.rl("textures/particle/laserball.png");
    private static final float ROTATION_SPEED = 5;
    private static final int SPIN_PERIOD = (int) (360 / ROTATION_SPEED);
    private static final float STEP = (float) Math.toRadians(60);
    private static final float HALF_SIZE = 0.25F;
    private static final float ALPHA = 0.8F;

    private final int color;
    /** Ticks added to the spin, so that balls appearing together do not turn in step. */
    private final float spinOffset;

    public PowerLaserBallParticle(ClientLevel level, BlockPos pos, int color) {
        super(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        this.color = ARGB.colorFromFloat(ALPHA, ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F);
        this.spinOffset = random.nextFloat() * random.nextInt(SPIN_PERIOD);
        this.lifetime = 6000;
        this.hasPhysics = false;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void emit(VertexConsumer consumer, Camera camera, float partialTicks) {
        float px = (float) (Mth.lerp(partialTicks, xo, x) - camera.position().x);
        float py = (float) (Mth.lerp(partialTicks, yo, y) - camera.position().y);
        float pz = (float) (Mth.lerp(partialTicks, zo, z) - camera.position().z);

        float spin = (float) Math.toRadians(
            ((level.getGameTime() + spinOffset) % SPIN_PERIOD) * ROTATION_SPEED + ROTATION_SPEED * partialTicks);

        // The same chain of rotations LP1 pushed on the GL matrix: each turn builds on the last.
        Matrix4f matrix = new Matrix4f().translation(px, py, pz).rotateY(spin).rotateZ(spin);
        Vector3f corner = new Vector3f();
        for (int t = 0; t < 3; t++) {
            matrix.rotateZ(STEP);
            for (int u = 0; u < 3; u++) {
                matrix.rotateX(STEP);
                for (int v = 0; v < 3; v++) {
                    matrix.rotateY(STEP);
                    vertex(consumer, matrix, corner, HALF_SIZE, HALF_SIZE, 1, 1);
                    vertex(consumer, matrix, corner, HALF_SIZE, -HALF_SIZE, 1, 0);
                    vertex(consumer, matrix, corner, -HALF_SIZE, -HALF_SIZE, 0, 0);
                    vertex(consumer, matrix, corner, -HALF_SIZE, HALF_SIZE, 0, 1);
                }
            }
        }
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f corner, float x, float y, float u,
        float v) {
        matrix.transformPosition(corner.set(x, y, 0));
        consumer.addVertex(corner.x, corner.y, corner.z).setUv(u, v).setColor(color);
    }
}
