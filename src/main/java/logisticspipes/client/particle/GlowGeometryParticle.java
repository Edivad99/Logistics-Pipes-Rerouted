package logisticspipes.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.LPConstants;
import logisticspipes.client.renderer.LPRenderTypes;

/**
 * Base for LP's power-laser effects: particles that draw arbitrary untextured geometry rather
 * than the single textured quad every vanilla particle is.
 *
 * <p>Up to 1.21.8 they got away with overriding {@code Particle#render} and fetching a buffer
 * source of their own inside it. 1.21.9 removed that hook: particles no longer draw themselves,
 * they are collected into a {@link net.minecraft.client.particle.ParticleGroup} which extracts a
 * render state once per frame and submits it. An unknown {@link ParticleRenderType} falls back to
 * the textured-quad group, which cannot express this geometry, so LP registers a group of its own
 * -- see {@link GlowParticleGroup} -- and every particle returning {@link #GROUP} lands in it.</p>
 */
public abstract class GlowGeometryParticle extends Particle {

    /**
     * The group key. Registered with a factory through {@code RegisterParticleGroupsEvent}; the
     * registration also decides where LP's particles fall in the frame's particle draw order.
     */
    public static final ParticleRenderType GROUP = new ParticleRenderType(LPConstants.ID + ":glow_geometry");

    protected GlowGeometryParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public ParticleRenderType getGroup() {
        return GROUP;
    }

    /**
     * Appends this particle's geometry as quads, in camera-relative world space -- the same
     * coordinates the old {@code render} override produced, since {@link LPRenderTypes#GLOW} is
     * drawn with the camera already at the origin.
     *
     * <p>Called during extraction, not at draw time: the vertices are snapshotted into the render
     * state so that a tick landing between extraction and the draw cannot move a particle
     * halfway through its own geometry.</p>
     */
    public abstract void emit(VertexConsumer consumer, Camera camera, float partialTicks);
}
