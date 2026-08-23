package logisticspipes.client.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;

import logisticspipes.client.renderer.LPRenderTypes;

/**
 * Additive draw state for the spark particle.
 *
 * <p>This has followed the particle pipeline down three rewrites. It started as a class
 * implementing {@code ParticleRenderType}, whose {@code begin} set the GL state by hand -- additive
 * blending, depth writes off, the particle shader and atlas. 1.21.4 turned {@code ParticleRenderType}
 * into a record wrapping a {@code RenderType}; 1.21.5 moved the shader, the blend function and the
 * write mask out of the composite state and into a {@code RenderPipeline}.</p>
 *
 * <p>1.21.9 finished the job. Particles no longer draw themselves: they extract into a
 * {@code QuadParticleRenderState}, which batches every quad of a frame into one buffer and then
 * issues one draw per {@link SingleQuadParticle.Layer}. So the layer is what carries the state now
 * -- atlas, pipeline, and whether the draw belongs to the translucent pass -- and
 * {@code ParticleRenderType} is back to being a bare name used only to pick the particle group.
 * A particle that just wants different blending stays in {@code SINGLE_QUADS} and returns its own
 * layer, which is what this is.</p>
 *
 * @see LPRenderTypes#ADDITIVE_PARTICLE_PIPELINE
 */
public final class SparkParticleRenderType {

    public static final SingleQuadParticle.Layer SPARK_LAYER = new SingleQuadParticle.Layer(
        true, TextureAtlas.LOCATION_PARTICLES, LPRenderTypes.ADDITIVE_PARTICLE_PIPELINE);

    private SparkParticleRenderType() {
    }
}
