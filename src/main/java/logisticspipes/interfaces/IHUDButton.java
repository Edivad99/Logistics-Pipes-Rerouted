package logisticspipes.interfaces;

import logisticspipes.client.renderer.hud.HUDDrawContext;

/**
 * A button on a HUD panel. It floats in the world, so it is pressed by looking at it rather than
 * with the mouse.
 */
public interface IHUDButton {

    int getX();

    int getY();

    int sizeX();

    int sizeY();

    /**
     * The player started looking at the button.
     */
    void setFocused();

    boolean isFocused();

    /**
     * The player looked away.
     */
    void clearFocused();

    /**
     * Holds the button as pressed, until {@link #clearFocused} releases it.
     */
    void blockFocused();

    boolean isBlockFocused();

    /**
     * @return the milliseconds since the button was focused
     */
    int focusedTime();

    void clicked();

    /**
     * Draws the button itself, when {@link #shouldRenderButton} says so.
     *
     * @param hover   whether the player is looking at it
     * @param clicked whether it is held as pressed
     * @param shifted whether the player is sneaking
     */
    void renderButton(HUDDrawContext context, boolean hover, boolean clicked, boolean shifted);

    /**
     * Draws what shows even while the button itself does not.
     */
    void renderAlways(HUDDrawContext context, boolean shifted);

    boolean shouldRenderButton();

    boolean buttonEnabled();
}
