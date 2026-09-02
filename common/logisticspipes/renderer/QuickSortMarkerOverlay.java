package logisticspipes.renderer;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import logisticspipes.utils.QuickSortChestMarkerStorage;

/**
 * Rings the slots a quicksort module is working on, in the screen of the inventory it sorts.
 *
 * <p>Restores the indicator that {@code modplugins.nei.DrawHandler} used to draw. That hooked
 * NEI's {@code IContainerDrawHandler}, which does not exist here; the same drawing rides
 * {@code ScreenEvent.Render.Post} instead, which needs no other mod and is where the slot finder's
 * overlay already lives.
 */
public final class QuickSortMarkerOverlay {

    /**
     * Vanilla's own hover ring, drawn behind the item so the stack stays readable.
     *
     * <p>The original drew a patch of {@code widgets.png} that no longer holds it. This sprite is
     * the same shape, and follows the resource pack.
     */
    private static final Identifier SLOT_HIGHLIGHT =
            Identifier.withDefaultNamespace("container/slot_highlight_back");

    private static final int RING_SIZE = 24;
    private static final int RING_OFFSET = 4;

    private QuickSortMarkerOverlay() {
    }

    public static void render(GuiGraphicsExtractor guiGraphics) {
        final QuickSortChestMarkerStorage storage = QuickSortChestMarkerStorage.getInstance();
        if (!storage.isActivated()
                || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        final Collection<Integer> marked = storage.getMarker();
        if (marked.isEmpty()) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            // The marks are slots of the sorted inventory, so the player's own are not candidates:
            // their indices overlap, and a match there would be a coincidence.
            if (slot.container == Minecraft.getInstance().player.getInventory()) {
                continue;
            }
            if (marked.contains(slot.index)) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT,
                        screen.getLeftPos() + slot.x - RING_OFFSET,
                        screen.getTopPos() + slot.y - RING_OFFSET,
                        RING_SIZE, RING_SIZE);
            }
        }
    }
}
