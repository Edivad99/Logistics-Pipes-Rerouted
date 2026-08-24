package logisticspipes.world.item;

import java.util.Objects;
import java.util.function.Consumer;

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

import logisticspipes.world.item.component.LPDataComponents;
import network.rs485.logisticspipes.util.TextUtil;

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
                    if (targetItem instanceof ItemModule) {
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForModule")));
                        tooltipAdder.accept(
                            Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
                    } else if (targetItem instanceof ItemUpgrade) {
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUpgrade")));
                        tooltipAdder.accept(
                            Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
                    } else if (targetItem instanceof ItemLogisticsPipe) {
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForPipe")));
                        tooltipAdder.accept(
                            Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
                    } else {
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
                        tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
                    }
                }
            } else {
                tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
                tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
                tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
            }
        }
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
    }
}
