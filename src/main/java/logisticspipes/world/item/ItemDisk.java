package logisticspipes.world.item;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemDisk extends LogisticsItem {

    public ItemDisk(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Gives a disk an empty data component if it has none yet.
     *
     * <p>A disk straight off the crafting table carries no {@code CUSTOM_DATA}, and the screens
     * that show one read it without checking. Doing this before the disk is sent means the client
     * always has something to read.
     *
     * @return the same stack, for chaining
     */
    public static ItemStack withData(ItemStack disk) {
        if (!disk.isEmpty() && disk.getItem() instanceof ItemDisk && !disk.has(DataComponents.CUSTOM_DATA)) {
            disk.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        }
        return disk;
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
