package logisticspipes.items;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemLogisticsProgrammer extends LogisticsItem {

	public static final String RECIPE_TARGET = "LogisticsRecipeTarget";

	public ItemLogisticsProgrammer() {
		super(new Properties().stacksTo(1));
	}

	@Nonnull
	@Override
	public ItemStack getCraftingRemainingItem(@Nonnull ItemStack itemStack) {
		ItemStack items = super.getCraftingRemainingItem(itemStack);
		items.setTag(itemStack.getTag());
		return items;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (!stack.isEmpty()) {
			if (stack.hasTag()) {
				CompoundTag nbt = stack.getTag();
				String target = nbt.getString(RECIPE_TARGET);
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
