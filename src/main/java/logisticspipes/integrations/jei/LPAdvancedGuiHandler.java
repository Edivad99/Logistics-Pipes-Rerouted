package logisticspipes.integrations.jei;

import java.util.List;

import net.minecraft.client.renderer.Rect2i;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;

import network.rs485.logisticspipes.gui.BaseGuiContainer;

/**
 * Exposes extra GUI areas (outside the main container window) to JEI
 * so it knows to move its ingredient panel out of the way.
 */
public class LPAdvancedGuiHandler implements IGuiContainerHandler<BaseGuiContainer<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(BaseGuiContainer<?> containerScreen) {
        return containerScreen.getExtraGuiAreas().stream()
            .map(r -> new Rect2i(r.getRoundedX(), r.getRoundedY(), r.getRoundedWidth(), r.getRoundedHeight()))
            .toList();
    }
}
