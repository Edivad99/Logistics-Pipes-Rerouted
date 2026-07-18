package logisticspipes.items;

import java.util.List;
import java.util.Objects;

import logisticspipes.world.item.component.LPDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemLogisticsProgrammer extends LogisticsItem {

	public ItemLogisticsProgrammer() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
		ItemStack result = new ItemStack(this);
		result.applyComponents(itemStack.getComponents());
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (!stack.isEmpty()) {
			if (stack.has(LPDataComponents.RECIPE_TARGET)) {
				String target = Objects.requireNonNull(stack.get(LPDataComponents.RECIPE_TARGET));
				if (!target.isEmpty()) {
					Item targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(target));
					if (targetItem instanceof ItemModule) {
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForModule")));
						tooltipComponents.add(Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
					} else if (targetItem instanceof ItemUpgrade) {
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUpgrade")));
						tooltipComponents.add(Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
					} else if (targetItem instanceof ItemLogisticsPipe) {
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForPipe")));
						tooltipComponents.add(Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
					} else {
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
						tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
					}
				}
			} else {
				tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
				tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
				tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
			}
		}
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}
}
