package logisticspipes.client.renderer;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * A {@link SubmitNodeCollector} that draws immediately into a {@link MultiBufferSource} instead of
 * queueing for the level renderer.
 *
 * <p>Item rendering lives behind the collector: there is no static entry point that takes a buffer
 * source any more. That is fine for a block entity renderer, which is handed a collector, but LP's
 * head-up display draws from {@code RenderLevelStageEvent}, which exposes only a {@code PoseStack}
 * -- no collector is reachable from a level-stage listener, in vanilla or in NeoForge.</p>
 *
 * <p>So the HUD keeps its buffer source and adapts in the other direction. The work is still
 * vanilla's: {@link #submitItem} is the body of {@code ItemFeatureRenderer.renderItem} minus the
 * foil and outline passes, which LP's HUD never asks for.</p>
 *
 * <p>Only the submissions LP actually produces are implemented. Everything else
 * throws rather than silently dropping geometry: if a future change starts routing models or name
 * tags through here, it should fail loudly and get a real answer, not render nothing.</p>
 */
public class ImmediateSubmitCollector implements SubmitNodeCollector {

    private final MultiBufferSource bufferSource;
    /** Per-quad colour/light/overlay carrier; reused, exactly as the vanilla renderer does. */
    private final QuadInstance quadInstance = new QuadInstance();

    public ImmediateSubmitCollector(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // Draw order here is call order; there is no queue to reorder.
        return this;
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
        int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        PoseStack.Pose pose = poseStack.last();
        quadInstance.setLightCoords(lightCoords);
        quadInstance.setOverlayCoords(overlayCoords);
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            // 26.1.2 moved the render type onto the quad: it is no longer an argument here, each
            // quad names the sheet it belongs to through its material.
            RenderType renderType = material.itemRenderType();
            int tint = material.tintIndex();
            quadInstance.setColor(tint >= 0 && tint < tintLayers.length ? tintLayers[tint] : -1);
            bufferSource.getBuffer(renderType).putBakedQuad(pose, quad, quadInstance);
        }
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
        SubmitNodeCollector.CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), bufferSource.getBuffer(renderType));
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(
            "ImmediateSubmitCollector does not implement " + what + "; it exists for LP's HUD item path only");
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        throw unsupported("submitShadow");
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 attachment, int offset, Component name,
        boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        throw unsupported("submitNameTag");
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
        Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        throw unsupported("submitText");
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        throw unsupported("submitFlame");
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        throw unsupported("submitLeash");
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
        int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        throw unsupported("submitModel");
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
        int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        throw unsupported("submitModelPart");
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        throw unsupported("submitMovingBlock");
    }

    /**
     * The body of {@code BlockFeatureRenderer.renderBlockModelSubmits} for a single submission,
     * minus the outline pass. Used by the side-config preview, which draws real block models into
     * its own buffer source.
     */
    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
        int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        quadInstance.setLightCoords(lightCoords);
        quadInstance.setOverlayCoords(overlayCoords);
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                putQuads(part.getQuads(direction), pose, tintLayers, buffer);
            }
            putQuads(part.getQuads(null), pose, tintLayers, buffer);
        }
    }

    private void putQuads(List<BakedQuad> quads, PoseStack.Pose pose, int[] tintLayers, VertexConsumer buffer) {
        for (BakedQuad quad : quads) {
            int tint = quad.materialInfo().tintIndex();
            quadInstance.setColor(tint >= 0 && tint < tintLayers.length ? tintLayers[tint] : -1);
            buffer.putBakedQuad(pose, quad, quadInstance);
        }
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        throw unsupported("submitBreakingBlockModel");
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
        throw unsupported("submitParticleGroup");
    }
}
