/*
 * Copyright (c) 2022  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2022  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.property;

import java.util.ArrayList;
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
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.SlotAccess;

// Container became Iterable<ItemStack> in 1.21.5, so this can no longer also declare
// Collection<ItemIdentifierStack>: the two Iterable parameterisations conflict. The collection-like
// members below stay as ordinary members; iteration over the identifier stacks is idStacks().
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

    public int getSize() {
        return getContainerSize();
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
        inv.clear();
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

    public boolean contains(ItemIdentifierStack element) {
        return inv.itemCount(element.getItem()) >= element.getStackSize();
    }

    public boolean containsAll(Collection<ItemIdentifierStack> elements) {
        Map<ItemIdentifier, Integer> items = inv.getItemsAndCount();
        for (ItemIdentifierStack element : elements) {
            Integer count = items.get(element.getItem());
            if (count == null || count < element.getStackSize()) {
                return false;
            }
        }
        return true;
    }

    public List<ItemIdentifierStack> idStacks() {
        List<ItemIdentifierStack> stacks = new ArrayList<>();
        inv.getItemsAndCount().forEach((item, count) -> stacks.add(item.makeStack(count)));
        return stacks;
    }

    /** Merges through the property so the change is observed, instead of straight to the inventory. */
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
    public void clearContent() {
        inv.clearContent();
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
    public Iterable<Pair<ItemIdentifierStack, Integer>> contents() {
        return inv.contents();
    }

    @Override
    public Object[] getTypeHolder() {
        return inv.getTypeHolder();
    }

    @Override
    public List<String> getClientInformation() {
        return inv.getClientInformation();
    }
}
