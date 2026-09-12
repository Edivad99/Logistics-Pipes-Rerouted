package logisticspipes.integrations.jei;

import java.util.List;

import net.minecraft.client.renderer.Rect2i;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;

import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;

/**
 * Exposes extra GUI areas (outside the main container window) to JEI
 * so it knows to move its ingredient panel out of the way.
 */
public class LPAdvancedGuiHandler implements IGuiContainerHandler<LogisticsBaseGuiScreen<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(LogisticsBaseGuiScreen<?> containerScreen) {
        return containerScreen.getGuiExtraAreas();
    }
}
