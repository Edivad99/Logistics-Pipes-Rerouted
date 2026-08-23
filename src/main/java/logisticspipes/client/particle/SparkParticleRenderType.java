package logisticspipes.client.particle;

import java.util.function.Function;

import net.minecraft.Util;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import logisticspipes.client.renderer.LPRenderTypes;

/**
 * Additive render type for the spark particle.
 *
 * <p>Was a class implementing {@code ParticleRenderType}, whose {@code begin} set the GL state by
 * hand -- additive blending, depth writes off, the particle shader and atlas. 1.21.4 turned
 * {@code ParticleRenderType} into a record wrapping a {@link RenderType}, so the state lives in the
 * render type's composite state instead and there is nothing left to implement. 1.21.5 then moved
 * the shader, the blend function and the write mask out of the composite state and into a
 * {@link net.minecraft.client.renderer.RenderPipeline}; see
 * {@link LPRenderTypes#ADDITIVE_PARTICLE_PIPELINE}.</p>
 *
 * <p>The composite mirrors vanilla's {@code translucent_particle} with two changes matching what
 * {@code begin} used to do: {@code LIGHTNING_TRANSPARENCY}, which is the SRC_ALPHA/ONE additive
 * blend the old code set explicitly, and {@code COLOR_WRITE}, which leaves the depth buffer alone
 * the way {@code depthMask(false)} did.</p>
 */
public final class SparkParticleRenderType {

    private static final Function<ResourceLocation, RenderType> ADDITIVE_PARTICLE = Util.memoize(
        texture -> RenderType.create(
            "lp_additive_particle",
            1536,
            false,
            false,
            LPRenderTypes.ADDITIVE_PARTICLE_PIPELINE,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                .setOutputState(RenderStateShard.PARTICLES_TARGET)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .createCompositeState(false)));

    public static final ParticleRenderType SPARK_PARTICLE_RENDER_TYPE = new ParticleRenderType(
        "SPARK_PARTICLE_RENDER_TYPE", ADDITIVE_PARTICLE.apply(TextureAtlas.LOCATION_PARTICLES), true);

    private SparkParticleRenderType() {
    }
}
