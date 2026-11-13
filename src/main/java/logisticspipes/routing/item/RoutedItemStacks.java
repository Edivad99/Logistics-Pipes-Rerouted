package logisticspipes.routing.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Carries an item's routing information on the stack itself, so the item keeps its destination
 * while another mod's pipes move it between two logistics pipes.
 */
public final class RoutedItemStacks {

    public static final String KEY = LPConstants.rl("routingdata").toString();

    private RoutedItemStacks() {
    }

    /**
     * A copy of the item {@code info} describes, carrying {@code info} with it.
     */
    public static ItemStack attach(ItemRoutingInformation info, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        info.storeToNBT(output);
        CompoundTag routingData = output.buildResult();
        ItemStack stack = info.getItem().makeNormalStack();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, routingData));
        return stack;
    }

    /**
     * Takes the routing information off {@code stack}, leaving the stack as it was before
     * {@link #attach}.
     *
     * @return the information, sized to the stack, or null when the stack carries none
     */
    public static @Nullable ItemRoutingInformation detach(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(KEY)) {
            return null;
        }
        CompoundTag routingData = tag.getCompoundOrEmpty(KEY);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, custom -> custom.remove(KEY));
        ItemRoutingInformation info = ItemRoutingInformation.restoreFromNBT(
            TagValueInput.create(ProblemReporter.DISCARDING, registries, routingData));
        // The other mod may have split the stack on the way
        info.setItem(ItemIdentifierStack.getFromStack(stack));
        return info;
    }
}
