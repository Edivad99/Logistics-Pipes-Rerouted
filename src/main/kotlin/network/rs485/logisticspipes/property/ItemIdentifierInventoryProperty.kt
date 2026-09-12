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

package network.rs485.logisticspipes.property

import logisticspipes.api.property.InventoryProperty
import logisticspipes.api.property.ObserverCallback
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory
import network.rs485.logisticspipes.inventory.SlotAccess
import logisticspipes.utils.item.ItemIdentifierInventory
import logisticspipes.utils.item.ItemIdentifierStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.concurrent.CopyOnWriteArraySet

// Container became Iterable<ItemStack> in 1.21.5, so this can no longer also declare
// Collection<ItemIdentifierStack>: the two Iterable parameterisations conflict. The collection-like
// members below stay as ordinary members; iteration over the identifier stacks is idStacks().
class ItemIdentifierInventoryProperty(private val inv: ItemIdentifierInventory, private val tagKey: String) :
    InventoryProperty<ItemIdentifierInventory>, IItemIdentifierInventory by inv {

    init {
        require(tagKey.isNotBlank()) { "tagKey must not be blank" }
    }

    override val slotAccess: SlotAccess = object : SlotAccess by inv.slotAccess {
        override fun mergeSlots(intoSlot: Int, fromSlot: Int) =
            inv.slotAccess.mergeSlots(intoSlot, fromSlot).also { iChanged() }
    }

    private val propertyObservers: CopyOnWriteArraySet<ObserverCallback<ItemIdentifierInventory>> =
        CopyOnWriteArraySet()

    override fun getTagKey(): String = tagKey

    override fun getPropertyObservers(): CopyOnWriteArraySet<ObserverCallback<ItemIdentifierInventory>> =
        propertyObservers

    val size: Int get() = containerSize

    override fun removeItem(index: Int, count: Int): ItemStack = inv.removeItem(index, count).also { iChanged() }

    override fun removeItemNoUpdate(index: Int): ItemStack = inv.removeItemNoUpdate(index).also { iChanged() }

    override fun setItem(index: Int, stack: ItemStack) =
        inv.setItem(index, stack).also { iChanged() }

    override fun setItem(i: Int, itemstack: ItemIdentifierStack?) =
        inv.setItem(i, itemstack).also { iChanged() }

    override fun handleItemIdentifierList(allItems: Collection<ItemIdentifierStack?>) =
        inv.handleItemIdentifierList(allItems).also { iChanged() }

    fun clear() = inv.clear().also { iChanged() }

    override fun recheckStackLimit() = inv.recheckStackLimit().also { iChanged() }

    override fun clearInventorySlotContents(i: Int) = inv.clearInventorySlotContents(i).also { iChanged() }

    override fun deserialize(input: ValueInput) {
        inv.deserialize(input, tagKey).also { iChanged() }
    }

    override fun serialize(output: ValueOutput) = inv.serialize(output, tagKey)

    override fun copyValue(): ItemIdentifierInventory = ItemIdentifierInventory(inv)

    override fun copyProperty(): ItemIdentifierInventoryProperty = ItemIdentifierInventoryProperty(copyValue(), tagKey)

    fun contains(element: ItemIdentifierStack): Boolean = inv.itemCount(element.item) >= element.getStackSize()

    fun containsAll(elements: Collection<ItemIdentifierStack>): Boolean = inv.itemsAndCount.let { items ->
        elements.all { items[it.item]?.run { compareTo(it.getStackSize()) >= 0 } ?: false }
    }

    fun idStacks(): List<ItemIdentifierStack> = inv.itemsAndCount.map { it.key.makeStack(it.value) }

}
