package logisticspipes.client.gui.tooltip;

import java.util.List;

import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import logisticspipes.world.item.ItemModule;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;

/**
 * Moves the module's item grid to where its {@code <inventory>} line was.
 * <p>
 * Vanilla puts the component from {@link net.minecraft.world.item.Item#getTooltipImage} right below
 * the item name, which would strand the label the module writes ahead of it ("Filter: ", "Supplied: ")
 * at the bottom of the tooltip with nothing under it.
 */
public final class ModuleTooltipPlacement {

    private ModuleTooltipPlacement() {
    }

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemModule)) {
            return;
        }
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        int gridIndex = indexOfGrid(elements);
        if (gridIndex < 0) {
            return;
        }
        int lineIndex = ItemModule.getInventoryLineIndex(stack);
        if (lineIndex < 0) {
            return;
        }
        Either<FormattedText, TooltipComponent> grid = elements.remove(gridIndex);
        // The item name always takes the first line, so the module's own lines start at index 1.
        elements.add(Math.min(lineIndex + 1, elements.size()), grid);
    }

    private static int indexOfGrid(List<Either<FormattedText, TooltipComponent>> elements) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).right().filter(ModuleInventoryTooltip.class::isInstance).isPresent()) {
                return i;
            }
        }
        return -1;
    }
}
