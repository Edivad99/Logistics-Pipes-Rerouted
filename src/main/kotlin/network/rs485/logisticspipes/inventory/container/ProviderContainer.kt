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

package network.rs485.logisticspipes.inventory.container

import network.rs485.logisticspipes.gui.widget.GhostItemSlot
import network.rs485.logisticspipes.gui.widget.GhostSlot
import network.rs485.logisticspipes.property.InventoryProperty
import network.rs485.logisticspipes.property.layer.PropertyLayer
import network.rs485.logisticspipes.property.layer.PropertyOverlayInventoryAdapter
import logisticspipes.modules.ModuleProvider
import logisticspipes.network.ModuleTarget
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

class ProviderContainer(
    menuType: MenuType<*>,
    containerId: Int,
    playerInventoryIn: Inventory,
    providerModule: ModuleProvider,
    target: ModuleTarget,
    moduleInHand: ItemStack,
) : LPBaseContainer<ModuleProvider>(menuType, containerId, providerModule, target) {

    /** The screen's edit buffer; see [ItemSinkContainer.propertyLayer]. */
    val propertyLayer = PropertyLayer(providerModule.propertyList)

    val playerSlots = addPlayerSlotsToContainer(playerInventoryIn, 0, 0, moduleInHand)

    val filterSlots = addDummySlotsToContainer(
        overlayInventory = PropertyOverlayInventoryAdapter(propertyLayer.overlay(providerModule.filterInventory)),
        baseProperty = module.filterInventory,
        startX = 0,
        startY = 0,
    )

    // Add 3x3 grid of dummy slots.
    override fun addDummySlotsToContainer(
        overlayInventory: Container,
        baseProperty: InventoryProperty<*>?,
        startX: Int,
        startY: Int
    ): List<GhostSlot> {
        val filterSlots = mutableListOf<GhostSlot>()

        for (row in 0..2) {
            for (column in 0..2) {
                filterSlots.add(
                    addGhostItemSlotToContainer(
                        dummyInventoryIn = overlayInventory,
                        baseProperty = baseProperty,
                        slotId = column + row * 3,
                        posX = startX + column * slotSize,
                        posY = startY + row * slotSize,
                    ),
                )
            }
        }

        return filterSlots
    }

    override fun tryTransferSlotToGhostSlot(slotIdx: Int): Boolean {
        val playerInvSlot = slots.getOrNull(slotIdx)?.takeIf { playerSlots.contains(it) } ?: return false
        var firstFreeSlotId = Int.MAX_VALUE
        for (filterSlot in filterSlots.withIndex()) {
            if (filterSlot.value.hasItem()) {
                if (ItemStack.isSameItem(filterSlot.value.item, playerInvSlot.item)) {
                    // item already in filter slots
                    return false
                }
            } else {
                firstFreeSlotId = minOf(firstFreeSlotId, filterSlot.index)
            }
        }

        return firstFreeSlotId.takeIf { it in filterSlots.indices }
            ?.let { filterSlots[it] as? GhostItemSlot }
            ?.let { firstFreeSlot ->
                applyItemStackToGhostItemSlot(playerInvSlot.item, firstFreeSlot)
                true
            }
            ?: false
    }

    override fun stillValid(playerIn: Player): Boolean = true

    override fun applyItemStackToGhostItemSlot(itemStack: ItemStack, slot: GhostSlot) {
        val copiedStack = itemStack.copy().apply { count = 1 }
        slot.set(copiedStack)
    }
}
