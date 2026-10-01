package logisticspipes.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Vector3f;

import logisticspipes.LPConstants;

/**
 * The beam of a power laser, running from a pipe along one of its sides: three textured quads crossed
 * at 60 degrees that spin about the beam while the texture slides along it, as LP1 drew it.
 */
@Accessors(chain = true)
public class PowerLaserBeamParticle extends GlowGeometryParticle {

    private static final Identifier TEXTURE = LPConstants.rl("textures/particle/laserbeam.png");
    private static final float ROTATION_SPEED = 5;
    private static final float HALF_WIDTH = 0.07F;
    private static final float ALPHA = 0.5F;

    /**
     * Whether the energy flows towards the pipe rather than away from it; only the texture's slide
     * follows it, the beam itself always runs along its direction.
     */
    @Setter
    private boolean reverse = false;

    private final float length;
    private final Vector3f axis;
    private final Vector3f across1;
    private final Vector3f across2;
    private final int color;
    /** Ticks added to the slide, so that beams laid at the same time do not pulse in step. */
    private final float slideOffset;

    public PowerLaserBeamParticle(ClientLevel level, BlockPos pos, float length, Direction dir, int color) {
        super(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        this.length = length;
        this.axis = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Direction perpendicular = dir.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        this.across1 = new Vector3f(perpendicular.getStepX(), perpendicular.getStepY(), perpendicular.getStepZ());
        this.across2 = new Vector3f(axis).cross(across1);
        this.color = ARGB.colorFromFloat(ALPHA, ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F);
        this.slideOffset = random.nextFloat() * random.nextInt(10);
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

        long time = level.getGameTime();
        float slide = time + slideOffset + partialTicks;
        if (reverse) {
            slide = -slide;
        }
        float textureSlide = -slide * 0.2F - Mth.floor(-slide * 0.1F);
        float spin = (time % (int) (360 / ROTATION_SPEED)) * ROTATION_SPEED + ROTATION_SPEED * partialTicks;

        for (int t = 0; t < 3; t++) {
            double angle = Math.toRadians(spin + 60 * (t + 1));
            float cos = (float) Math.cos(angle) * HALF_WIDTH;
            float sin = (float) Math.sin(angle) * HALF_WIDTH;
            // Half the width of the quad, turned about the beam.
            float wx = across1.x * cos + across2.x * sin;
            float wy = across1.y * cos + across2.y * sin;
            float wz = across1.z * cos + across2.z * sin;
            float ex = px + axis.x * length;
            float ey = py + axis.y * length;
            float ez = pz + axis.z * length;
            float v0 = -1.0F + textureSlide + t / 3.0F;
            float v1 = v0 + length;

            consumer.addVertex(ex - wx, ey - wy, ez - wz).setUv(1, v1).setColor(color);
            consumer.addVertex(px - wx, py - wy, pz - wz).setUv(1, v0).setColor(color);
            consumer.addVertex(px + wx, py + wy, pz + wz).setUv(0, v0).setColor(color);
            consumer.addVertex(ex + wx, ey + wy, ez + wz).setUv(0, v1).setColor(color);
        }
    }
}
