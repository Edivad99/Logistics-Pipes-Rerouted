package logisticspipes.world.item.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * The filter inventory of a module, as it is stored in the item's {@code moduleInformation} tag.
 * The stacks stay unparsed here because {@link net.minecraft.world.item.Item#getTooltipImage} has
 * no access to the registries needed to read them; the client component decodes them instead.
 *
 * @param moduleInformation the module's saved NBT
 * @param prefix            key prefix the inventory was written under
 * @param size              number of slots in the inventory
 */
public record ModuleInventoryTooltip(CompoundTag moduleInformation, String prefix, int size)
    implements TooltipComponent {
}
