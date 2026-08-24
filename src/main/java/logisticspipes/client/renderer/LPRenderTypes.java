package logisticspipes.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;

import java.util.function.Function;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import logisticspipes.LPConstants;

/**
 * The render pipelines and types LP's untextured world geometry draws through.
 *
 * <p>Up to 1.21.4 these draws set their own GL state — {@code RenderSystem.setShader},
 * {@code enableBlend}, {@code defaultBlendFunc}, {@code depthMask}, {@code disableDepthTest} —
 * and pushed the mesh straight to the GPU with {@code BufferUploader.drawWithShader}. 1.21.5
 * removed all of it: state is now declared once in a {@link RenderPipeline}, and geometry goes
 * through a {@link RenderType} and a buffer source. Both pipelines below reuse vanilla's
 * {@code core/position_color} shader through {@link RenderPipelines#DEBUG_FILLED_SNIPPET},
 * which already carries the translucent blend function and the POSITION_COLOR/QUADS format.</p>
 */
public final class LPRenderTypes {

    private LPRenderTypes() {
    }

    /**
     * Translucent, depth-tested but not depth-writing, drawn from both sides. Reproduces the
     * {@code enableBlend + defaultBlendFunc + depthMask(false)} the laser particles used to set
     * by hand; culling is off because they are crossed billboard quads seen from either face.
     */
    public static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/glow"))
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();

    /**
     * As {@link #GLOW_PIPELINE}, but with the depth test off so the geometry draws over the
     * world — what {@code RenderSystem.disableDepthTest()} bought the HUD's routing lasers.
     */
    public static final RenderPipeline OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/overlay"))
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();

    /**
     * Additive particle blending with the depth buffer left alone -- what the spark particle used
     * to get from {@code RenderStateShard.PARTICLE_SHADER} plus {@code LIGHTNING_TRANSPARENCY} plus
     * {@code COLOR_WRITE}. Those shards are gone: shader, blend and write mask are pipeline
     * properties now. {@code BlendFunction.LIGHTNING} is the same SRC_ALPHA/ONE function the
     * lightning transparency shard applied.
     */
    public static final RenderPipeline ADDITIVE_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/additive_particle"))
        .withBlend(BlendFunction.LIGHTNING)
        .withDepthWrite(false)
        .build();

    /**
     * Translucent entity rendering with back-face culling left on -- vanilla's
     * {@code ENTITY_TRANSLUCENT} turns culling off, which is the one thing the ghost pipe preview
     * did not want.
     */
    public static final RenderPipeline GHOST_ENTITY_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/ghost_entity"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withSampler("Sampler1")
        .withBlend(BlendFunction.TRANSLUCENT)
        .build();

    /**
     * Textured, translucent, drawn over whatever is already there. Replaces
     * {@code RenderType.guiTexturedOverlay}, which 1.21.6 removed along with every other
     * {@code RenderType.gui*}: the side-config preview needs it outside a GuiGraphics, so LP has to
     * declare the pipeline itself.
     */
    public static final RenderPipeline TEXTURED_OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/textured_overlay"))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build();

    /**
     * Flat coloured geometry for the world-space HUD panels: translucent, depth-tested so the world
     * still occludes them, but not depth-writing. The panels stack many coplanar layers within a
     * fraction of a block, and letting each one write depth makes the next fail the LEQUAL test and
     * come out stippled -- the problem the old GuiGraphics path worked around by spreading the
     * layers apart in z.
     */
    public static final RenderPipeline HUD_FILL_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/hud_fill"))
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();

    /** As {@link #HUD_FILL_PIPELINE}, textured. */
    public static final RenderPipeline HUD_TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(LPConstants.rl("pipeline/hud_textured"))
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();

    private static final int BUFFER_SIZE = 1536;

    public static final RenderType GLOW = RenderType.create(
        LPConstants.ID + ":glow",
        RenderSetup.builder(GLOW_PIPELINE)
            .bufferSize(BUFFER_SIZE)
            .createRenderSetup());

    public static final RenderType OVERLAY = RenderType.create(
        LPConstants.ID + ":overlay",
        RenderSetup.builder(OVERLAY_PIPELINE)
            .bufferSize(BUFFER_SIZE)
            .createRenderSetup());

    /**
     * Pipelines have to be handed to the pipeline registry before anything can draw through
     * them; wired up from {@code ClientManager}.
     */
    public static final RenderType HUD_FILL = RenderType.create(
        LPConstants.ID + ":hud_fill",
        RenderSetup.builder(HUD_FILL_PIPELINE)
            .bufferSize(BUFFER_SIZE)
            .createRenderSetup());

    /** Textured HUD geometry bound to a given texture; memoized so each texture keeps one type. */
    public static final Function<Identifier, RenderType> HUD_TEXTURED = Util.memoize(
        texture -> RenderType.create(
            LPConstants.ID + ":hud_textured",
            RenderSetup.builder(HUD_TEXTURED_PIPELINE)
                .bufferSize(BUFFER_SIZE)
                .withTexture("Sampler0", texture)
                .createRenderSetup()));

    /** Textured overlay bound to a given texture; memoized so each texture keeps one type. */
    public static final Function<Identifier, RenderType> TEXTURED_OVERLAY = Util.memoize(
        texture -> RenderType.create(
            LPConstants.ID + ":textured_overlay",
            RenderSetup.builder(TEXTURED_OVERLAY_PIPELINE)
                .bufferSize(BUFFER_SIZE)
                .withTexture("Sampler0", texture)
                .createRenderSetup()));

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GLOW_PIPELINE);
        event.registerPipeline(OVERLAY_PIPELINE);
        event.registerPipeline(ADDITIVE_PARTICLE_PIPELINE);
        event.registerPipeline(GHOST_ENTITY_PIPELINE);
        event.registerPipeline(TEXTURED_OVERLAY_PIPELINE);
        event.registerPipeline(HUD_FILL_PIPELINE);
        event.registerPipeline(HUD_TEXTURED_PIPELINE);
    }
}
