/*
 * Copyright (c) 2023  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2023  RS485
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

package logisticspipes.property;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.utils.item.SimpleStackInventory;

public class SimpleInventoryProperty implements Property<SimpleStackInventory>, Container {

    private final SimpleStackInventory inv;

    private final String tagKey;

    private final CopyOnWriteArraySet<ObserverCallback<SimpleStackInventory>> propertyObservers =
        new CopyOnWriteArraySet<>();

    public SimpleInventoryProperty(SimpleStackInventory inv, String tagKey) {
        this.inv = inv;
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<SimpleStackInventory>> getPropertyObservers() {
        return propertyObservers;
    }

    @Override
    public SimpleStackInventory copyValue() {
        return new SimpleStackInventory(inv);
    }

    @Override
    public Property<SimpleStackInventory> copyProperty() {
        return new SimpleInventoryProperty(copyValue(), tagKey);
    }

    @Override
    public void deserialize(ValueInput input) {
        inv.deserialize(input, tagKey);
    }

    @Override
    public void serialize(ValueOutput output) {
        inv.serialize(output, tagKey);
    }

    public void clearInventorySlotContents(int i) {
        inv.clearInventorySlotContents(i);
        iChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        inv.dropContents(level, pos);
        iChanged();
    }

    public int addCompressed(ItemStack toAdd, boolean ignoreMaxStackSize) {
        int added = inv.addCompressed(toAdd, ignoreMaxStackSize);
        iChanged();
        return added;
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
    public void setChanged() {
        inv.setChanged();
        iChanged();
    }

    public void clear() {
        inv.clearContent();
        iChanged();
    }

    /** @deprecated do not change the returned ItemStack, and do not call markDirty afterwards */
    @Deprecated
    @Override
    public ItemStack getItem(int index) {
        return inv.getItem(index);
    }

    public boolean isSlotEmpty(int index) {
        return inv.getItem(index).isEmpty();
    }

    /**
     * @see SimpleStackInventory#stackStream()
     */
    public Stream<ItemStack> stackStream() {
        return inv.stackStream();
    }

    // ── plain delegation ─────────────────────────────────────────────────────
    // Only Container's abstract members: its default members stay inherited, so they keep running
    // against this property and therefore through the notifying overrides above.

    @Override
    public int getContainerSize() {
        return inv.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.stillValid(player);
    }

    @Override
    public void clearContent() {
        inv.clearContent();
    }
}
