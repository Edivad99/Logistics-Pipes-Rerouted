package logisticspipes.inventory;

import java.util.Collection;
import java.util.Map;

import net.minecraft.world.Container;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public interface IItemIdentifierInventory extends Container, IClientInformationProvider {

    Map<ItemIdentifier, Integer> getItemsAndCount();

    SlotAccess getSlotAccess();

    String getName();

    @Nullable ItemIdentifierStack getIDStackInSlot(int i);

    void setItem(int i, @Nullable ItemIdentifierStack itemstack);

    boolean containsItem(@Nullable ItemIdentifier item);

    /**
     * Slot by slot, with a null standing in for every empty slot.
     */
    void handleItemIdentifierList(Collection<@Nullable ItemIdentifierStack> allItems);

    void addListener(ISimpleInventoryEventHandler listener);

    void removeListener(ISimpleInventoryEventHandler listener);

    boolean containsUndamagedItem(ItemIdentifier item);

    boolean containsExcludeNBTItem(ItemIdentifier item);

    boolean containsUndamagedExcludeNBTItem(ItemIdentifier item);

    int itemCount(ItemIdentifier item);

    /** Every slot in order, empty ones included. */
    Iterable<SlotContent> contents();

    void recheckStackLimit();

    void clearInventorySlotContents(int i);

    record SlotContent(int slot, @Nullable ItemIdentifierStack stack) {}
}
