package logisticspipes.property;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.InventoryProperty;
import logisticspipes.api.property.ObserverCallback;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemIdentifierInventoryProperty
    implements InventoryProperty<ItemIdentifierInventory>, IItemIdentifierInventory {

    private final ItemIdentifierInventory inv;

    private final String tagKey;

    private final SlotAccess slotAccess;

    private final CopyOnWriteArraySet<ObserverCallback<ItemIdentifierInventory>> propertyObservers =
        new CopyOnWriteArraySet<>();

    public ItemIdentifierInventoryProperty(ItemIdentifierInventory inv, String tagKey) {
        if (tagKey.isBlank()) {
            throw new IllegalArgumentException("tagKey must not be blank");
        }
        this.inv = inv;
        this.tagKey = tagKey;
        this.slotAccess = new NotifyingSlotAccess();
    }

    @Override
    public SlotAccess getSlotAccess() {
        return slotAccess;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<ItemIdentifierInventory>> getPropertyObservers() {
        return propertyObservers;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = inv.removeItem(index, count);
        iChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = inv.removeItemNoUpdate(index);
        iChanged();
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inv.setItem(index, stack);
        iChanged();
    }

    @Override
    public void setItem(int i, @Nullable ItemIdentifierStack itemstack) {
        inv.setItem(i, itemstack);
        iChanged();
    }

    @Override
    public void handleItemIdentifierList(Collection<@Nullable ItemIdentifierStack> allItems) {
        inv.handleItemIdentifierList(allItems);
        iChanged();
    }

    public void clear() {
        clearContent();
    }

    @Override
    public void clearContent() {
        inv.clearContent();
        iChanged();
    }

    @Override
    public void recheckStackLimit() {
        inv.recheckStackLimit();
        iChanged();
    }

    @Override
    public void clearInventorySlotContents(int i) {
        inv.clearInventorySlotContents(i);
        iChanged();
    }

    @Override
    public void deserialize(ValueInput input) {
        inv.deserialize(input, tagKey);
        iChanged();
    }

    @Override
    public void serialize(ValueOutput output) {
        inv.serialize(output, tagKey);
    }

    @Override
    public ItemIdentifierInventory copyValue() {
        return new ItemIdentifierInventory(inv);
    }

    @Override
    public ItemIdentifierInventoryProperty copyProperty() {
        return new ItemIdentifierInventoryProperty(copyValue(), tagKey);
    }

    /**
     * Merges through the property so the change is observed, instead of straight to the inventory.
     */
    private class NotifyingSlotAccess implements SlotAccess {

        @Override
        public void mergeSlots(int intoSlot, int fromSlot) {
            inv.getSlotAccess().mergeSlots(intoSlot, fromSlot);
            iChanged();
        }

        @Override
        public boolean canMerge(int intoSlot, int fromSlot) {
            return inv.getSlotAccess().canMerge(intoSlot, fromSlot);
        }

        @Override
        public boolean isSlotEmpty(int idx) {
            return inv.getSlotAccess().isSlotEmpty(idx);
        }
    }

    // ── plain delegation ─────────────────────────────────────────────────────
    // Only the abstract members of the delegated interfaces: their default members stay inherited,
    // so they keep running against this property and therefore through the notifying overrides.

    @Override
    public int getContainerSize() {
        return inv.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return inv.getItem(index);
    }

    @Override
    public void setChanged() {
        inv.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.stillValid(player);
    }

    @Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() {
        return inv.getItemsAndCount();
    }

    @Override
    public String getName() {
        return inv.getName();
    }

    @Override
    public @Nullable ItemIdentifierStack getIDStackInSlot(int i) {
        return inv.getIDStackInSlot(i);
    }

    @Override
    public boolean containsItem(@Nullable ItemIdentifier item) {
        return inv.containsItem(item);
    }

    @Override
    public void addListener(ISimpleInventoryEventHandler listener) {
        inv.addListener(listener);
    }

    @Override
    public void removeListener(ISimpleInventoryEventHandler listener) {
        inv.removeListener(listener);
    }

    @Override
    public boolean containsUndamagedItem(ItemIdentifier item) {
        return inv.containsUndamagedItem(item);
    }

    @Override
    public boolean containsExcludeNBTItem(ItemIdentifier item) {
        return inv.containsExcludeNBTItem(item);
    }

    @Override
    public boolean containsUndamagedExcludeNBTItem(ItemIdentifier item) {
        return inv.containsUndamagedExcludeNBTItem(item);
    }

    @Override
    public int itemCount(ItemIdentifier item) {
        return inv.itemCount(item);
    }

    @Override
    public Iterable<SlotContent> contents() {
        return inv.contents();
    }

    @Override
    public List<String> getClientInformation() {
        return inv.getClientInformation();
    }
}
