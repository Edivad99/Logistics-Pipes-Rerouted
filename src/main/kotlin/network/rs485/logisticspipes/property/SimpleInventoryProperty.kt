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

package network.rs485.logisticspipes.property

import logisticspipes.utils.item.SimpleStackInventory
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.concurrent.CopyOnWriteArraySet
import java.util.stream.Stream

class SimpleInventoryProperty(private val inv: SimpleStackInventory, override val tagKey: String) :
    Property<SimpleStackInventory>, Container by inv {

    override val propertyObservers: CopyOnWriteArraySet<ObserverCallback<SimpleStackInventory>> =
        CopyOnWriteArraySet()

    override fun copyValue(): SimpleStackInventory = SimpleStackInventory(inv)

    override fun copyProperty(): Property<SimpleStackInventory> = SimpleInventoryProperty(copyValue(), tagKey)

    override fun deserialize(input: ValueInput) = inv.deserialize(input, tagKey)

    override fun serialize(output: ValueOutput) = inv.serialize(output, tagKey)

    fun clearInventorySlotContents(i: Int) = inv.clearInventorySlotContents(i).alsoIChanged()

    fun dropContents(level: Level, pos: BlockPos) = inv.dropContents(level, pos).alsoIChanged()

    fun addCompressed(toAdd: ItemStack, ignoreMaxStackSize: Boolean): Int =
        inv.addCompressed(toAdd, ignoreMaxStackSize).alsoIChanged()

    override fun removeItem(index: Int, count: Int): ItemStack = inv.removeItem(index, count).alsoIChanged()

    override fun removeItemNoUpdate(index: Int): ItemStack = inv.removeItemNoUpdate(index).alsoIChanged()

    override fun setItem(index: Int, stack: ItemStack) =
        inv.setItem(index, stack).alsoIChanged()

    override fun setChanged() = inv.setChanged().alsoIChanged()

    fun clear() = inv.clearContent().alsoIChanged()

    @Deprecated("do not change returned ItemStack or call markDirty afterwards")
    override fun getItem(index: Int): ItemStack = inv.getItem(index)

    fun isSlotEmpty(index: Int): Boolean = inv.getItem(index).isEmpty

    /**
     * @see SimpleStackInventory.stackStream
     */
    fun stackStream(): Stream<ItemStack> {
        return inv.stackStream()
    }
}
