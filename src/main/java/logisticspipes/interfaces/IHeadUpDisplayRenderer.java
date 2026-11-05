package logisticspipes.interfaces;

import net.minecraft.client.Minecraft;

import logisticspipes.client.renderer.hud.HUDDrawContext;
import logisticspipes.world.item.component.HUDComponent;

/**
 * Draws a HUD panel and reacts to where the player is looking on it.
 */
public interface IHeadUpDisplayRenderer {

    /**
     * @param context  the HUD panel draw target, with the billboard transform already applied to its
     *                 pose. Passed down by the caller instead of being read from a global, so the HUD
     *                 render path carries no dependency on ambient state.
     * @param distance how far the panel is from the player
     * @param shifted  whether the player is sneaking
     * @param config   what the player's glasses are set to show
     */
    void renderHeadUpDisplay(HUDDrawContext context, double distance, boolean day, boolean shifted,
        Minecraft minecraft, HUDComponent config);

    /**
     * @return whether the panel shows at all with this configuration
     */
    boolean display(HUDComponent config);

    boolean cursorOnWindow(int x, int y);

    void handleCursor(int x, int y);
}
