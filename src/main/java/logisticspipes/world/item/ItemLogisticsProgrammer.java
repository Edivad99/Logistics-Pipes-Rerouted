package logisticspipes.world.item;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import logisticspipes.Translations;
import logisticspipes.world.item.component.LPDataComponents;

public class ItemLogisticsProgrammer extends LogisticsItem {

    public ItemLogisticsProgrammer(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return new ItemStackTemplate(this, 1, componentPatchOf(instance));
    }

    private static DataComponentPatch componentPatchOf(ItemInstance instance) {
        return switch (instance) {
            case ItemStack stack -> stack.getComponentsPatch();
            case ItemStackTemplate template -> template.components();
            default -> DataComponentPatch.EMPTY;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        if (!stack.isEmpty()) {
            if (stack.has(LPDataComponents.RECIPE_TARGET)) {
                String target = Objects.requireNonNull(stack.get(LPDataComponents.RECIPE_TARGET));
                if (!target.isEmpty()) {
                    Item targetItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(target));
                    final ChatFormatting firstLine = ChatFormatting.GRAY;
                    final ChatFormatting secondLine = ChatFormatting.DARK_AQUA;
                    switch (targetItem) {
                        case ItemModule _ -> {
                            tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_MODULE).withStyle(firstLine));
                            tooltipAdder.accept(Component.translatable(targetItem.getDescriptionId()).withStyle(secondLine));
                        }
                        case ItemUpgrade _ -> {
                            tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_UPGRADE).withStyle(firstLine));
                            tooltipAdder.accept(Component.translatable(targetItem.getDescriptionId()).withStyle(secondLine));
                        }
                        case ItemLogisticsPipe _ -> {
                            tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_PIPE).withStyle(firstLine));
                            tooltipAdder.accept(Component.translatable(targetItem.getDescriptionId()).withStyle(secondLine));
                        }
                        default -> addEmptyTooltip(tooltipAdder);
                    }
                }
            } else {
                addEmptyTooltip(tooltipAdder);
            }
        }
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
    }

    private static void addEmptyTooltip(Consumer<Component> tooltipAdder) {
        tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_EMPTY_1).withStyle(ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_EMPTY_2).withStyle(ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable(Translations.Tooltip.PROGRAMMER_EMPTY_3).withStyle(ChatFormatting.RED));
    }
}
