package logisticspipes.utils.gui;

import net.minecraft.world.item.ItemStack;

/**
 * The item tooltip to draw for whatever is currently under the cursor, or null when nothing is
 * hovered.
 *
 * @param screenX the cursor position in <b>screen</b> coordinates, ready to hand to
 * {@code GuiGraphicsExtractor#renderTooltip}. Render from a {@code renderToolTips} override, which both
 * screen bases call outside any pose translation; drawing from {@code extractLabels} instead runs
 * inside a pose already translated by (leftPos, topPos) and offsets the tooltip by the gui origin.
 * @param screenY see {@code screenX}.
 * @param stack the hovered stack.
 */
public record ItemTooltip(int screenX, int screenY, ItemStack stack) {}
