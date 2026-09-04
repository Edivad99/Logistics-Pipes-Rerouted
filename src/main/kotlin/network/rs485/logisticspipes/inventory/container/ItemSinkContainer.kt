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

import network.rs485.logisticspipes.gui.widget.GhostSlot
import network.rs485.logisticspipes.property.InventoryProperty
import network.rs485.logisticspipes.property.layer.PropertyLayer
import network.rs485.logisticspipes.property.layer.PropertyOverlayInventoryAdapter
import network.rs485.logisticspipes.util.FuzzyFlag
import logisticspipes.modules.ModuleItemSink
import logisticspipes.network.ModuleTarget
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import java.util.*

class ItemSinkContainer(
    menuType: MenuType<*>,
    containerId: Int,
    playerInventory: Inventory,
    itemSinkModule: ModuleItemSink,
    target: ModuleTarget,
    val isFuzzy: Boolean,
    moduleInHand: ItemStack,
) : LPBaseContainer<ModuleItemSink>(menuType, containerId, itemSinkModule, target) {

    private val flags = EnumSet.of(FuzzyFlag.IGNORE_NBT, FuzzyFlag.IGNORE_DAMAGE)

    /**
     * The buffer the screen edits: a property is only written back to the module when the screen
     * closes. The server builds one too and throws it away, which is what keeps a click on a ghost
     * slot from reaching the module behind the player's back.
     */
    val propertyLayer = PropertyLayer(itemSinkModule.properties)

    val fuzzyFlagOverlay = propertyLayer.overlay(itemSinkModule.fuzzyFlags)

    val playerSlots = addPlayerSlotsToContainer(
        playerInventoryIn = playerInventory,
        startX = 0,
        startY = 0,
        lockedStack = moduleInHand,
    )

    val filterSlots = addDummySlotsToContainer(
        overlayInventory = PropertyOverlayInventoryAdapter(propertyLayer.overlay(itemSinkModule.filterInventory)),
        baseProperty = module.filterInventory,
        startX = 0,
        startY = 0,
    )

    override fun addDummySlotsToContainer(
        overlayInventory: Container,
        baseProperty: InventoryProperty<*>?,
        startX: Int,
        startY: Int
    ): List<GhostSlot> {
        val filterSlots = mutableListOf<GhostSlot>()

        for (column in 0 until 9) {
            filterSlots.add(
                if (isFuzzy) {
                    addFuzzyItemSlotToContainer(
                        dummyInventoryIn = overlayInventory,
                        baseProperty = baseProperty,
                        slotId = column,
                        posX = startX + column * slotSize,
                        posY = startY,
                        usedFlags = flags,
                    ) {
                        fuzzyFlagOverlay.read { p ->
                            p.get(column * 4, column * 4 + 3)
                        }
                    }
                } else {
                    addGhostItemSlotToContainer(
                        dummyInventoryIn = overlayInventory,
                        baseProperty = baseProperty,
                        slotId = column,
                        posX = startX + column * slotSize,
                        posY = startY,
                    )
                },
            )
        }

        return filterSlots
    }

    override fun stillValid(playerIn: Player): Boolean = true

}
