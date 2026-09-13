/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
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

package network.rs485.logisticspipes.module

import logisticspipes.modules.AsyncModule

import network.rs485.logisticspipes.inventory.IItemIdentifierInventory
import logisticspipes.api.property.BooleanProperty
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty
import logisticspipes.api.property.Property
import network.rs485.logisticspipes.util.matchingSequence
import logisticspipes.gui.hud.modules.HUDAdvancedExtractor
import logisticspipes.interfaces.*
import logisticspipes.modules.SimpleFilter
import logisticspipes.modules.SneakyDirection
import logisticspipes.network.ModuleTarget
import logisticspipes.network.to_client.module.AdvancedExtractorIncludeMessage
import logisticspipes.network.to_client.module.ModuleInventoryMessage
import logisticspipes.proxy.computers.interfaces.CCCommand
import logisticspipes.utils.ISimpleInventoryEventHandler
import logisticspipes.utils.item.ItemIdentifierInventory
import logisticspipes.utils.item.ItemIdentifierStack
import logisticspipes.world.inventory.AdvancedExtractorMenu
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import kotlinx.coroutines.Deferred


class AsyncAdvancedExtractor : AsyncModule<ExtractorJob, Unit>(), SimpleFilter, SneakyDirection,
    IClientInformationProvider, IHUDModuleHandler, IModuleWatchReceiver, IModuleInventoryReceive,
    ISimpleInventoryEventHandler, IModuleMenuProvider {

    companion object {
        @JvmStatic
        val name: String = "extractor_advanced"
    }

    private val filterInventory = ItemIdentifierInventoryProperty(ItemIdentifierInventory(9, "Item list", 1), "filterInv")
    val itemsIncluded = BooleanProperty(true, "itemsIncluded")
    override fun getProperties(): List<Property<*>> = extractor.properties + listOf(filterInventory, itemsIncluded)

    private val hud = HUDAdvancedExtractor(this)
    private val extractor = AsyncExtractorModule(
        inverseFilter = {
            it.isEmpty || itemsIncluded.value != filterInventory.matchingSequence(it).any()
        },
    )

    override fun getSneakyDirection(): Direction? = extractor.getSneakyDirection()

    override fun setSneakyDirection(direction: Direction?) {
        extractor.setSneakyDirection(direction)
    }

    override fun getEveryNthTick(): Int = extractor.everyNthTick

    override fun createMenu(containerId: Int, inventory: Inventory, target: ModuleTarget): AbstractContainerMenu =
        AdvancedExtractorMenu(containerId, inventory, target, this)

    override fun writeMenuData(buffer: RegistryFriendlyByteBuf) {
        buffer.writeBoolean(itemsIncluded.value)
    }

    override fun finishInit() {
        val isInitialized = super.initialized
        super.finishInit()
        if (isInitialized) return
        if (service != null) {
            val level = worldProvider?.getLevel()
            if (level?.isClientSide == false) {
                itemsIncluded.addObserver {
                    extractor.localModeWatchers.send(
                        AdvancedExtractorIncludeMessage(ModuleTarget.of(this), it.copyValue()),
                    )
                }
            }
        }
    }

    override fun getLPName(): String = name

    override fun registerHandler(world: ILevelProvider?, service: IPipeServiceProvider?) {
        super.registerHandler(world, service)
        extractor.registerHandler(world, service)
    }

    override fun registerPosition(slot: ModulePositionType, positionInt: Int) {
        super.registerPosition(slot, positionInt)
        extractor.registerPosition(slot, positionInt)
    }

    override fun receivePassive(): Boolean = false

    override fun hasGenericInterests(): Boolean = false

    override fun interestedInUndamagedID(): Boolean = false

    override fun interestedInAttachedInventory(): Boolean = false

    override fun jobSetup(): ExtractorJob = extractor.jobSetup()

    override fun completeJob(result: Unit?) = extractor.completeJob(result)

    override fun tickAsync(setupObject: ExtractorJob) = extractor.tickAsync(setupObject)

    override fun runSyncWork() = extractor.runSyncWork()

    @CCCommand(description = "Returns the FilterInventory of this Module")
    override fun getFilterInventory(): IItemIdentifierInventory {
        return filterInventory
    }

    override fun handleInvContent(items: MutableCollection<ItemIdentifierStack?>) =
        filterInventory.handleItemIdentifierList(items)

    override fun InventoryChanged(inventory: Container) {
        if (world?.isClientSide == false) {
            extractor.localModeWatchers.send(
                ModuleInventoryMessage(
                    ModuleTarget.of(this),
                    ItemIdentifierStack.getListFromInventory(inventory),
                ),
            )
        }
    }

    override fun getClientInformation(): MutableList<String> {
        val clientInformation = extractor.clientInformation
        clientInformation.add(if (itemsIncluded.value) "Included" else "Excluded")
        // The pair the tooltip looks for: it turns the filter into an item grid instead of the
        // raw stack strings addAll(filterInventory.clientInformation) used to leave here.
        clientInformation.add("<inventory>")
        clientInformation.add("<that>" + filterInventory.tagKey)
        return clientInformation
    }

    override fun startWatching(player: Player) {
        extractor.startWatching(player)
        if (player is ServerPlayer) {
            val target = ModuleTarget.of(this)
            PacketDistributor.sendToPlayer(
                player,
                ModuleInventoryMessage(target, ItemIdentifierStack.getListFromInventory(filterInventory)),
            )
            PacketDistributor.sendToPlayer(
                player,
                AdvancedExtractorIncludeMessage(target, itemsIncluded.value),
            )
        }
    }

    override fun stopWatching(player: Player) = extractor.stopWatching(player)


    override fun getHUDRenderer(): IHUDModuleRenderer? = hud


}
