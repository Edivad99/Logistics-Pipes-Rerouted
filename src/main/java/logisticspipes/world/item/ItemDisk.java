package logisticspipes.world.item;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemDisk extends LogisticsItem {

    public ItemDisk(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
        TooltipFlag tooltipFlag) {
        if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
            final CompoundTag tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
            if (tag.contains("name")) {
                String name = "\u00a78" + tag.getString("name");
                tooltipComponents.add(Component.literal(name));
            }
        }
    }
}
