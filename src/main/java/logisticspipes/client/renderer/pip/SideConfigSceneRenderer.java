package logisticspipes.client.renderer.pip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;

public class SideConfigSceneRenderer extends PictureInPictureRenderer<SideConfigSceneState> {

    public SideConfigSceneRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<SideConfigSceneState> getRenderStateClass() {
        return SideConfigSceneState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "logistics side config";
    }

    @Override
    protected void renderToTexture(SideConfigSceneState state, PoseStack poseStack) {
        // The texture is the scene rectangle in real pixels, which is the size prepare() gave it.
        final int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        state.display().renderToTexture(poseStack, bufferSource,
                (state.x1() - state.x0()) * guiScale, (state.y1() - state.y0()) * guiScale);
    }
}
