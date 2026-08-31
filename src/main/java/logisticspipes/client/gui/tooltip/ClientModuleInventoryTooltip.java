package logisticspipes.client.gui.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;

/**
 * Draws a module's filter inventory as a grid of item slots, the way a bundle shows its contents,
 * instead of listing the stacks as tooltip text. Reuses the vanilla bundle sprites so the grid
 * follows the resource pack.
 */
public class ClientModuleInventoryTooltip implements ClientTooltipComponent {

    private static final Identifier SLOT_SPRITE =
        Identifier.withDefaultNamespace("container/bundle/slot_background");

    /** The sprite is a 24x24 nine-slice with a 4px border, so the item sits in the inner 16x16. */
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_BORDER = 4;
    private static final int MARGIN_Y = 4;

    /** Filter inventories are laid out nine wide in the GUIs, so the tooltip matches that. */
    private static final int MAX_COLUMNS = 9;

    private final List<ItemStack> items;
    private final int columns;
    private final int rows;

    public ClientModuleInventoryTooltip(ModuleInventoryTooltip tooltip) {
        items = readItems(tooltip);
        columns = Math.clamp(items.size(), 1, MAX_COLUMNS);
        rows = Math.max(1, Mth.positiveCeilDiv(items.size(), columns));
    }

    private static List<ItemStack> readItems(ModuleInventoryTooltip tooltip) {
        Level level = Minecraft.getInstance().level;
        if (level == null || tooltip.size() <= 0) {
            return List.of();
        }
        HolderLookup.Provider registries = level.registryAccess();
        ItemIdentifierInventory inventory =
            new ItemIdentifierInventory(tooltip.size(), "InformationTempInventory", Integer.MAX_VALUE);
        inventory.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries,
            tooltip.moduleInformation()), tooltip.prefix());
        List<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            stacks.add(inventory.getItem(slot));
        }
        return stacks;
    }

    @Override
    public int getHeight(Font font) {
        return backgroundHeight() + MARGIN_Y;
    }

    @Override
    public int getWidth(Font font) {
        return backgroundWidth();
    }

    private int backgroundWidth() {
        return columns * SLOT_SIZE;
    }

    private int backgroundHeight() {
        return rows * SLOT_SIZE;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        for (int index = 0; index < items.size(); index++) {
            int slotX = x + index % columns * SLOT_SIZE;
            int slotY = y + index / columns * SLOT_SIZE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            ItemStack stack = items.get(index);
            if (!stack.isEmpty()) {
                graphics.item(stack, slotX + SLOT_BORDER, slotY + SLOT_BORDER, index);
                graphics.itemDecorations(font, stack, slotX + SLOT_BORDER, slotY + SLOT_BORDER);
            }
        }
    }
}
