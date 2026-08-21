package logisticspipes.world.item;

import java.util.List;
import java.util.function.Consumer;
import java.util.Objects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemDisk extends LogisticsItem {

    public ItemDisk(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
            final CompoundTag tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
            if (tag.contains("name")) {
                String name = "\u00a78" + tag.getStringOr("name", "");
                tooltipAdder.accept(Component.literal(name));
            }
        }
    }
}
