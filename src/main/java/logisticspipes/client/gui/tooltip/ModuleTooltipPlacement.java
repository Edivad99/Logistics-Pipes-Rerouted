package logisticspipes.client.gui.tooltip;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import com.mojang.datafixers.util.Either;

import logisticspipes.world.item.ItemModule;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;

/**
 * Moves the module's item grid under the line that introduces it.
 * <p>
 * Vanilla puts the component from {@code getTooltipImage} right below the item name, which strands
 * the label the module writes ahead of it ("Filter: ", "Supplied: ") <em>under</em> the grid.
 * <p>
 * The grid is placed by finding that label in the tooltip rather than by counting lines. The
 * module's information list and the finished tooltip are not the same list -- the tooltip also
 * holds the item name and anything the stack's components contribute -- so an index into one says
 * nothing about the other.
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
        String label = ItemModule.getInventoryLabel(stack);
        if (label == null) {
            return;
        }
        int labelIndex = indexOfText(elements, label);
        if (labelIndex < 0) {
            return;
        }
        Either<FormattedText, TooltipComponent> grid = elements.remove(gridIndex);
        // Removing the grid shifts everything after it down one.
        if (labelIndex > gridIndex) {
            labelIndex--;
        }
        elements.add(labelIndex + 1, grid);
    }

    private static int indexOfGrid(List<Either<FormattedText, TooltipComponent>> elements) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).right().filter(ModuleInventoryTooltip.class::isInstance).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfText(List<Either<FormattedText, TooltipComponent>> elements, String text) {
        for (int i = 0; i < elements.size(); i++) {
            Optional<FormattedText> line = elements.get(i).left();
            if (line.isPresent() && plainText(line.get()).equals(text)) {
                return i;
            }
        }
        return -1;
    }

    private static String plainText(FormattedText text) {
        StringBuilder builder = new StringBuilder();
        text.visit(part -> {
            builder.append(part);
            return Optional.empty();
        });
        return builder.toString();
    }
}
