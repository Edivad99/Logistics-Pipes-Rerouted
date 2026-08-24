package logisticspipes.client.renderer;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/**
 * A {@link SubmitNodeCollector} that draws immediately into a {@link MultiBufferSource} instead of
 * queueing for the level renderer.
 *
 * <p>1.21.9 moved item rendering behind the collector: {@code ItemRenderer.renderStatic} is gone
 * and {@code ItemStackRenderState} can only {@code submit}. That is fine for a block entity
 * renderer, which is handed a collector, but LP's head-up display draws from
 * {@code RenderLevelStageEvent}, which still exposes only a {@code PoseStack} -- there is no
 * collector anywhere in reach of a level-stage listener, in vanilla or in NeoForge.</p>
 *
 * <p>So the HUD keeps its buffer source and adapts in the other direction. The work itself is
 * still vanilla's: {@link ItemRenderer#renderItem} is public, static, and takes a
 * {@code MultiBufferSource}, so an item submitted here is drawn by exactly the code the level
 * renderer would have reached eventually.</p>
 *
 * <p>Only the two submissions LP's item path actually produces are implemented. Everything else
 * throws rather than silently dropping geometry: if a future change starts routing models or name
 * tags through here, it should fail loudly and get a real answer, not render nothing.</p>
 */
public class ImmediateSubmitCollector implements SubmitNodeCollector {

    private final MultiBufferSource bufferSource;

    public ImmediateSubmitCollector(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // Draw order here is call order; there is no queue to reorder.
        return this;
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay,
        int outlineColor, int[] tintLayers, List<BakedQuad> quads, RenderType renderType,
        ItemStackRenderState.FoilType foilType) {
        ItemRenderer.renderItem(displayContext, poseStack, bufferSource, light, overlay, tintLayers, quads,
            renderType, foilType);
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
    public void submitShadow(PoseStack poseStack, float strength, List<EntityRenderState.ShadowPiece> pieces) {
        throw unsupported("submitShadow");
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 offset, int backgroundColor, Component text,
        boolean visibleThroughWalls, int light, double distanceSqr, CameraRenderState cameraState) {
        throw unsupported("submitNameTag");
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow,
        Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
        throw unsupported("submitText");
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState entityState, Quaternionf rotation) {
        throw unsupported("submitFlame");
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        throw unsupported("submitLeash");
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
        int light, int overlay, int color, @Nullable TextureAtlasSprite sprite, int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        throw unsupported("submitModel");
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int light, int overlay,
        @Nullable TextureAtlasSprite sprite, boolean skipRoot, boolean foil, int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling, int color) {
        throw unsupported("submitModelPart");
    }

    @Override
    public void submitBlock(PoseStack poseStack, BlockState blockState, int light, int overlay, int outlineColor) {
        throw unsupported("submitBlock");
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state) {
        throw unsupported("submitMovingBlock");
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model,
        float red, float green, float blue, int light, int overlay, int outlineColor) {
        throw unsupported("submitBlockModel");
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
        throw unsupported("submitParticleGroup");
    }
}
