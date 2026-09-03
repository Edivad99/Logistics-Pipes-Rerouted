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

import network.rs485.grow.Coroutines
import network.rs485.logisticspipes.logistics.LogisticsManager
import network.rs485.logisticspipes.property.Property
import network.rs485.logisticspipes.util.equalsWithNBT
import network.rs485.logisticspipes.util.getExtractionMax
import logisticspipes.LPConfigs
import logisticspipes.interfaces.IInventoryUtil
import logisticspipes.modules.PipeServiceProviderUtil
import logisticspipes.network.ModuleTarget
import logisticspipes.network.to_client.module.QuickSortStateMessage
import logisticspipes.particle.Particles
import logisticspipes.pipes.basic.CoreRoutedPipe
import logisticspipes.routing.AsyncRouting
import logisticspipes.routing.ServerRouter
import logisticspipes.utils.PlayerCollectionList
import logisticspipes.utils.SinkReply
import logisticspipes.utils.item.ItemIdentifier
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

const val STALLED_DELAY = 24
const val NORMAL_DELAY = 6

data class QuicksortAsyncResult(
    val slot: Int,
    val itemid: ItemIdentifier,
    val destRouterId: Int,
    val sinkReply: SinkReply,
)

class AsyncQuicksortModule : AsyncModule<Pair<Int, ItemStack>?, QuicksortAsyncResult?>() {
    companion object {
        @JvmStatic
        val name: String = "quick_sort"
    }

    private val localSlotWatchers = PlayerCollectionList()
    private var stalled = true
    private var currentSlot = 0
        set(value) {
            field = value
            localSlotWatchers.send(QuickSortStateMessage(ModuleTarget.of(this), value))
        }
    private var stallSlot = 0

    override val properties: List<Property<*>>
        get() = emptyList()

    private val energyPerStack: Int
        get() = upgradeManager.let { 500 + 1000 * it.itemStackExtractionUpgrade }.toInt()
    override val everyNthTick: Int
        get() = if (stalled) STALLED_DELAY else NORMAL_DELAY

    override fun getLPName(): String = name

    override fun jobSetup(): Pair<Int, ItemStack>? {
        val serverRouter = this.service?.router as? ServerRouter ?: return null
        val inventory = service?.let { PipeServiceProviderUtil.availableInventories(it) }?.firstOrNull() ?: return null
        if (inventory.containerSize == 0) return null
        if (currentSlot >= inventory.containerSize) currentSlot = 0
        val slot = currentSlot++
        val stack = inventory.getItem(slot)
        if (!stalled && slot == stallSlot) stalled = true
        if (stack.isEmpty) return null
        AsyncRouting.updateServerRouterLsa(serverRouter)
        if (!LPConfigs.COMMON.DISABLE_ASYNC_WORK.asBoolean && AsyncRouting.needsRoutingTableUpdate(serverRouter)) {
            // go async
            return slot to stack
        }
        val itemid = ItemIdentifier.get(stack)
        val result = LogisticsManager.getDestination(stack, itemid, false, serverRouter, emptyList()) ?: return null
        extractAndSend(slot, stack, inventory, result.first, result.second)
        return null
    }

    override suspend fun tickAsync(setupObject: Pair<Int, ItemStack>?): QuicksortAsyncResult? {
        if (setupObject == null) return null
        val serverRouter = this.service?.router as? ServerRouter ?: return null
        AsyncRouting.updateRoutingTable(serverRouter)
        val itemid = ItemIdentifier.get(setupObject.second)
        val result = withContext(Coroutines.serverScope.coroutineContext) {
            LogisticsManager.getDestination(setupObject.second, itemid, false, serverRouter, emptyList())
        } ?: return null
        return QuicksortAsyncResult(setupObject.first, itemid, result.first, result.second)
    }

    @ExperimentalCoroutinesApi
    override fun completeJob(deferred: Deferred<QuicksortAsyncResult?>) {
        val result = deferred.getCompleted() ?: return
        val inventory = service?.let { PipeServiceProviderUtil.availableInventories(it) }?.firstOrNull() ?: return
        if (result.slot >= inventory.containerSize) return
        val stack = inventory.getItem(result.slot)
        if (result.itemid.equalsWithNBT(stack)) {
            extractAndSend(result.slot, stack, inventory, result.destRouterId, result.sinkReply)
        }
    }

    private fun extractAndSend(
        slot: Int,
        stack: ItemStack,
        inventory: IInventoryUtil,
        destRouterId: Int,
        sinkReply: SinkReply,
    ) {
        val service = service ?: return
        val pointedOrientation = service.pointedOrientation ?: return
        val toExtract = getExtractionMax(stack.count, stack.maxStackSize, sinkReply)
        if (toExtract <= 0) return
        if (!service.useEnergy(energyPerStack)) return
        stalled = false
        stallSlot = slot
        val extracted = inventory.removeItem(slot, toExtract)
        if (extracted.isEmpty) return
        service.sendStack(
            extracted,
            destRouterId,
            sinkReply,
            CoreRoutedPipe.ItemSendMode.Fast,
            pointedOrientation,
        )
        service.spawnParticle(Particles.ORANGE_SPARKLE, 8)
    }

    override fun runSyncWork() {}

    override fun receivePassive(): Boolean = false

    override fun hasGenericInterests(): Boolean = false

    override fun interestedInUndamagedID(): Boolean = false

    override fun interestedInAttachedInventory(): Boolean = false

    fun addWatchingPlayer(player: Player) {
        localSlotWatchers.add(player)
        if (player is ServerPlayer) {
            PacketDistributor.sendToPlayer(player, QuickSortStateMessage(ModuleTarget.of(this), currentSlot))
        }
    }

    fun removeWatchingPlayer(player: Player) {
        localSlotWatchers.remove(player)
    }

}
