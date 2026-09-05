package logisticspipes.client.renderer.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;

public record SideConfigSceneState(SideConfigDisplay display, int x0, int y0, int x1, int y1, float scale,
        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState {

    public SideConfigSceneState(SideConfigDisplay display, int x0, int y0, int x1, int y1,
            @Nullable ScreenRectangle scissorArea) {
        this(display, x0, y0, x1, y1, 1.0f, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
