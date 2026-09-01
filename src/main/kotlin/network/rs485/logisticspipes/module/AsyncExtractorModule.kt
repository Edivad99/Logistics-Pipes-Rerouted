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

import network.rs485.logisticspipes.logistics.LogisticsManager
import network.rs485.logisticspipes.property.NullableEnumProperty
import network.rs485.logisticspipes.property.Property
import network.rs485.logisticspipes.util.equalsWithNBT
import network.rs485.logisticspipes.util.getExtractionMax
import logisticspipes.LPConfigs
import logisticspipes.interfaces.*
import logisticspipes.network.NewGuiHandler
import logisticspipes.network.PacketHandler
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider
import logisticspipes.network.guis.module.inhand.SneakyModuleInHandGuiProvider
import logisticspipes.network.guis.module.inpipe.SneakyModuleInSlotGuiProvider
import logisticspipes.network.ModuleTarget
import logisticspipes.network.to_client.SneakyDirectionMessage
import logisticspipes.network.to_server.ModuleWatchMessage
import logisticspipes.particle.Particles
import logisticspipes.pipes.basic.CoreRoutedPipe
import logisticspipes.proxy.MainProxy
import logisticspipes.renderer.HUDDrawContext
import logisticspipes.routing.AsyncRouting
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.PlayerCollectionList
import logisticspipes.utils.item.ItemIdentifier
import logisticspipes.utils.item.ItemIdentifierStack
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import java.util.Optional
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

class ExtractorJob(private val module: AsyncExtractorModule, private val inventoryGetter: () -> IInventoryUtil?) {
    private var inventorySize = inventoryGetter()?.containerSize ?: 0
    private val slotsPerTick: Int = determineSlotsPerTick(module.everyNthTick, inventorySize)
    private val slotStartIter =
        if (slotsPerTick == 0) emptyList<Int>().iterator()
        else IntProgression.fromClosedRange(0, inventorySize - 1, slotsPerTick).iterator()
    private var itemsLeft = module.itemsToExtract
    private var stacksLeft = module.stacksToExtract
    private val updateRoutingTableMsgChannel: Channel<Unit> = Channel(Channel.CONFLATED)
    private val slotItemsToExtract: MutableMap<Int, ItemIdentifierStack> = HashMap()

    fun runSyncWork() {
        try {
            val serverRouter = module.serverRouter
            val inventory = inventoryGetter()
            if (slotsPerTick == 0 || !slotStartIter.hasNext() || serverRouter == null || inventory == null) {
                updateRoutingTableMsgChannel.close()
                return
            }
            inventorySize = inventory.containerSize
            val startSlot = slotStartIter.next()
            val stopSlot = if (slotStartIter.hasNext()) {
                min(inventorySize, startSlot + slotsPerTick)
            } else inventorySize

            val slotRange = startSlot until stopSlot
            for (slot in slotRange) {
                val stack = inventory.getItem(slot)
                if (module.inverseFilter(stack)) continue // filters the stack out by the given filter method
                val toExtract = min(itemsLeft, stack.count)
                itemsLeft -= toExtract
                --stacksLeft
                slotItemsToExtract[slot] = ItemIdentifierStack(ItemIdentifier.get(stack), toExtract)
                if (itemsLeft < 1) break
                if (stacksLeft < 1) break
            }
            AsyncRouting.updateServerRouterLsa(serverRouter)
            if (AsyncRouting.needsRoutingTableUpdate(serverRouter)) {
                updateRoutingTableMsgChannel.trySend(Unit)
            } else {
                extractAndSend(serverRouter, inventory)
            }
            if (!slotStartIter.hasNext()) {
                updateRoutingTableMsgChannel.close()
            }
        } catch (error: Exception) {
            updateRoutingTableMsgChannel.close(error)
            throw error
        }
    }

    suspend fun runAsyncWork() {
        if (!LPConfigs.COMMON.DISABLE_ASYNC_WORK.asBoolean) {
            updateRoutingTableMsgChannel.consumeAsFlow().collect {
                module.serverRouter?.also { serverRouter ->
                    AsyncRouting.updateRoutingTable(serverRouter)
                }
            }
        }
    }

    fun extractAndSend(serverRouter: ServerRouter, inventory: IInventoryUtil) {
        slotItemsToExtract.forEach { (slot, itemIdStack) ->
            extractAndSendStack(serverRouter, inventory, slot, itemIdStack)
        }
        slotItemsToExtract.clear()
    }

    private fun extractAndSendStack(
        serverRouter: ServerRouter,
        inventory: IInventoryUtil,
        slot: Int,
        itemIdStack: ItemIdentifierStack,
    ) {
        val service = module.pipeService ?: return
        val pointedOrientation = service.pointedOrientation ?: return
        val stack = inventory.getItem(slot)
        if (!itemIdStack.item.equalsWithNBT(stack)) return
        var sourceStackLeft = itemIdStack.getStackSize()
        val validDestinationSequence = LogisticsManager.allDestinations(
            stack = stack,
            itemid = ItemIdentifier.get(stack),
            canBeDefault = true,
            sourceRouter = serverRouter,
        ) { sourceStackLeft > 0 }
        validDestinationSequence.forEach { (destRouterId, sinkReply) ->
            var extract = getExtractionMax(stack.count, sourceStackLeft, sinkReply)
            if (extract < 1) return@forEach
            while (!service.useEnergy(module.energyPerItem * extract)) {
                service.spawnParticle(Particles.ORANGE_SPARKLE, 2)
                if (extract < 2) break
                extract /= 2
            }
            val toSend = inventory.removeItem(slot, extract)
            if (toSend.isEmpty) return@forEach
            service.sendStack(toSend, destRouterId, sinkReply, module.itemSendMode, pointedOrientation)
            sourceStackLeft -= toSend.count
        }
    }
}

class AsyncExtractorModule(
    val inverseFilter: (ItemStack) -> Boolean = { stack -> stack.isEmpty },
) : AsyncModule<ExtractorJob, Unit>(), Gui, SneakyDirection,
    IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver {

    companion object {
        @JvmStatic
        val name: String = "extractor"
    }

    private val sneakyDirectionProp = NullableEnumProperty(null, "sneakydirection", Direction.values())

    override val properties: List<Property<*>>
        get() = listOf(sneakyDirectionProp)

    override var sneakyDirection: Direction?
        get() = sneakyDirectionProp.value
        set(value) {
            sneakyDirectionProp.value = value
            localModeWatchers.send(SneakyDirectionMessage(ModuleTarget.of(this), Optional.ofNullable(value)))
        }

    private val hudRenderer: IHUDModuleRenderer = HUDAsyncExtractor(this)
    val localModeWatchers = PlayerCollectionList()
    override val module = this
    private var currentJob: ExtractorJob? = null

    internal val serverRouter: ServerRouter?
        get() = service?.router as? ServerRouter

    internal val pipeService: IPipeServiceProvider?
        get() = service

    override val pipeGuiProvider: ModuleCoordinatesGuiProvider
        get() =
            NewGuiHandler.getGui(SneakyModuleInSlotGuiProvider::class.java).setSneakyOrientation(sneakyDirection)

    override val inHandGuiProvider: ModuleInHandGuiProvider
        get() =
            NewGuiHandler.getGui(SneakyModuleInHandGuiProvider::class.java)

    override val everyNthTick: Int
        get() = (80 / upgradeManager.let { 2.0.pow(it.actionSpeedUpgrade) }).toInt() + LPConfigs.COMMON.MINIMUM_JOB_TICK_LENGTH.asInt

    val stacksToExtract: Int
        get() = 1 + upgradeManager.itemStackExtractionUpgrade

    val itemsToExtract: Int
        get() = upgradeManager.let { 4 * it.itemExtractionUpgrade + 64 * upgradeManager.itemStackExtractionUpgrade }
            .coerceAtLeast(1)

    internal val energyPerItem: Int
        get() = upgradeManager.let {
            5 * 1.1.pow(it.itemExtractionUpgrade) * 1.2.pow(it.itemStackExtractionUpgrade)
        }.toInt()

    internal val itemSendMode: CoreRoutedPipe.ItemSendMode
        get() = upgradeManager.let { um ->
            CoreRoutedPipe.ItemSendMode.Fast.takeIf { um.itemExtractionUpgrade > 0 }
        } ?: CoreRoutedPipe.ItemSendMode.Normal

    private val connectedInventory: IInventoryUtil?
        get() = service?.availableSneakyInventories(sneakyDirection)?.firstOrNull()

    override fun getLPName(): String = name

    override fun jobSetup(): ExtractorJob = ExtractorJob(this) { connectedInventory }.also {
        currentJob = it
        it.runSyncWork()
    }

    override fun runSyncWork() {
        currentJob?.runSyncWork()
    }

    override suspend fun tickAsync(setupObject: ExtractorJob) {
        setupObject.runAsyncWork()
    }

    override fun completeJob(deferred: Deferred<Unit?>) {
        val serverRouter = module.serverRouter ?: return
        val inventory = connectedInventory ?: return
        serverRouter.ensureLatestRoutingTable()
        currentJob?.extractAndSend(serverRouter, inventory)
    }

    override fun receivePassive(): Boolean = false

    override fun hasGenericInterests(): Boolean = false

    override fun interestedInUndamagedID(): Boolean = false

    override fun interestedInAttachedInventory(): Boolean = true

    override fun getClientInformation(): MutableList<String> =
        mutableListOf("Extraction: ${sneakyDirection?.name ?: "DEFAULT"}")


    override fun getHUDRenderer(): IHUDModuleRenderer = hudRenderer


    override fun startWatching(player: Player) {
        localModeWatchers.add(player)
        if (player is ServerPlayer) {
            PacketDistributor.sendToPlayer(
                player,
                SneakyDirectionMessage(ModuleTarget.of(this), Optional.ofNullable(sneakyDirection)),
            )
        }
    }

    override fun stopWatching(player: Player) {
        if (localModeWatchers.contains(player)) localModeWatchers.remove(player)
    }

    class HUDAsyncExtractor(private val module: AsyncExtractorModule) : IHUDModuleRenderer {
        override fun renderContent(context: HUDDrawContext, shifted: Boolean) {
            val mc = Minecraft.getInstance()

            val d: Direction? = module.sneakyDirection
            // TODO: deferred -- this panel has never drawn anything; the sneaky direction still
            // needs a line of text through context.drawString.
        }

        override fun getButtons(): List<IHUDButton> = emptyList()

    }

}
