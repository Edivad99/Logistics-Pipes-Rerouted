package logisticspipes.client.particle;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

/**
 * Additive render type for the spark particle.
 *
 * <p>Was a class implementing {@code ParticleRenderType}, whose {@code begin} set the GL state by
 * hand -- additive blending, depth writes off, the particle shader and atlas. 1.21.4 turned
 * {@code ParticleRenderType} into a record wrapping a {@link RenderType}, so the state lives in the
 * render type's composite state instead and there is nothing left to implement.</p>
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
            DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.PARTICLE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                .setOutputState(RenderStateShard.PARTICLES_TARGET)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false)));

    public static final ParticleRenderType SPARK_PARTICLE_RENDER_TYPE = new ParticleRenderType(
        "SPARK_PARTICLE_RENDER_TYPE", ADDITIVE_PARTICLE.apply(TextureAtlas.LOCATION_PARTICLES), true);

    private SparkParticleRenderType() {
    }
}
