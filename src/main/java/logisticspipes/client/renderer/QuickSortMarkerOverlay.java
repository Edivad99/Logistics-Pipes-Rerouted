package logisticspipes.client.renderer;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

import logisticspipes.utils.QuickSortChestMarkerStorage;

/**
 * Rings the slots a quicksort module is working on, in the screen of the inventory it sorts.
 */
public final class QuickSortMarkerOverlay {

    private static final Identifier HOTBAR_SELECTION =
            Identifier.withDefaultNamespace("hud/hotbar_selection");

    private static final int SPRITE_WIDTH = 24;
    private static final int SPRITE_HEIGHT = 23;

    private static final int INSET = 1;
    private static final int FRAME_SIZE = 22;

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
        Container playerContainer = Minecraft.getInstance().player.getInventory();
        for (Slot slot : screen.getMenu().slots) {
            // The marks are slots of the sorted inventory, so the player's own are not candidates:
            // their indices overlap, and a match there would be a coincidence.
            if (slot.container == playerContainer) {
                continue;
            }
            if (marked.contains(slot.index)) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION,
                        SPRITE_WIDTH, SPRITE_HEIGHT, INSET, INSET,
                        screen.getLeftPos() + slot.x - 3,
                        screen.getTopPos() + slot.y - 3,
                        FRAME_SIZE, FRAME_SIZE);
            }
        }
    }

}
