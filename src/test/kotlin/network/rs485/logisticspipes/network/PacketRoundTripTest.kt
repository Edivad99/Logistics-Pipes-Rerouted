/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2026  RS485
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

package network.rs485.logisticspipes.network

import io.netty.buffer.Unpooled
import logisticspipes.modules.LogisticsModule.ModulePositionType
import logisticspipes.network.abstractpackets.CoordinatesPacket
import logisticspipes.network.abstractpackets.ModernPacket
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket
import logisticspipes.network.packets.PlayerList
import logisticspipes.network.packets.RequestUpdateNamesPacket
import logisticspipes.network.packets.block.SecurityStationCCIDs
import logisticspipes.network.packets.orderer.DiscContent
import logisticspipes.network.packets.pipe.CraftingPriority
import logisticspipes.network.packets.pipe.FireWallFlag
import logisticspipes.network.packets.pipe.FluidSupplierAmount
import logisticspipes.network.packets.pipe.InvSysConSetChannelOnPipePacket
import logisticspipes.network.packets.pipe.ItemAmountSignUpdatePacket
import logisticspipes.network.packets.pipe.PipeContentRequest
import logisticspipes.network.packets.pipe.SendQueueContent
import logisticspipes.network.packets.pipe.SlotFinderActivatePacket
import net.minecraft.SharedConstants
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import logisticspipes.util.LPDataIOWrapper
import org.junit.jupiter.api.BeforeAll
import java.util.BitSet
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trip coverage for the packet serialization chain.
 *
 * LP packets serialize by inheritance: a leaf's `writeData` calls `super.writeData` up to
 * [ModernPacket], and the read side has to mirror that walk exactly. Nothing checked that
 * before, and an asymmetry does not throw -- it silently leaves bytes in the buffer and
 * desyncs whatever is read next out of the same packet.
 *
 * So the assertion that carries the weight here is not "the values came back", it is
 * **"the buffer is empty afterwards"**. One concrete packet per abstract base is enough:
 * what is under test is the chain, not the leaves.
 */
class PacketRoundTripTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }

        private const val ANY_ID = 0
    }

    private fun registries(): RegistryAccess =
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    /**
     * Writes [packet], reads it back into a fresh instance from [ModernPacket.template], and
     * fails if a single byte is left over.
     */
    private fun <P : ModernPacket> roundTrip(packet: P): P {
        val registries = registries()
        val data = LPDataIOWrapper.collectData(registries) { output -> packet.writeData(output) }

        @Suppress("UNCHECKED_CAST")
        val restored = packet.template() as P
        val buffer = Unpooled.wrappedBuffer(data)
        try {
            LPDataIOWrapper.provideData(buffer, registries) { input -> restored.readData(input) }
            assertEquals(
                0,
                buffer.readableBytes(),
                "${packet.javaClass.simpleName}: readData left bytes unread, so its read and " +
                    "write walks of the inheritance chain disagree",
            )
        } finally {
            buffer.release()
        }
        return restored
    }

    /** Fills the [ModernPacket] level of the chain. */
    private fun <P : ModernPacket> P.withDimension(): P = apply {
        setDimension(Identifier.parse("logisticspipes:test_dimension"))
    }

    /** Fills the [CoordinatesPacket] level of the chain. */
    private fun <P : CoordinatesPacket> P.withCoords(): P = apply {
        withDimension()
        posX = 12
        posY = -34
        posZ = 5678
    }

    /** Fills the [ModuleCoordinatesPacket] level of the chain. */
    private fun <P : ModuleCoordinatesPacket> P.withModule(): P = apply {
        withCoords()
        type = ModulePositionType.IN_PIPE
        positionInt = 3
    }

    private fun assertCoords(actual: CoordinatesPacket) {
        assertEquals(12, actual.posX)
        assertEquals(-34, actual.posY)
        assertEquals(5678, actual.posZ)
        assertEquals(Identifier.parse("logisticspipes:test_dimension"), actual.dimension)
    }

    private fun assertModule(actual: ModuleCoordinatesPacket) {
        assertCoords(actual)
        assertEquals(ModulePositionType.IN_PIPE, actual.type)
        assertEquals(3, actual.positionInt)
    }

    // ── level 1: ModernPacket ────────────────────────────────────────────────

    @Test
    fun `ModernPacket carries the dimension`() {
        // Any packet that chains into ModernPacket.writeData picks the dimension up; PlayerList
        // is the shallowest one that does.
        val actual = roundTrip(PlayerList(ANY_ID).withDimension())
        assertEquals(Identifier.parse("logisticspipes:test_dimension"), actual.dimension)
    }

    @Test
    fun `a packet that overrides writeData without super carries no payload at all`() {
        // RequestUpdateNamesPacket deliberately writes nothing -- not even the dimension, since
        // it never chains into ModernPacket. Pinning that down so the day someone "fixes" the
        // empty override, the asymmetry it would introduce is caught here and not in multiplayer.
        val registries = registries()
        val data = LPDataIOWrapper.collectData(registries) { output ->
            RequestUpdateNamesPacket(ANY_ID).withDimension().writeData(output)
        }
        assertEquals(0, data.size, "RequestUpdateNamesPacket is expected to serialize to nothing")
    }

    @Test
    fun `StringListPacket round trip`() {
        val expected = listOf("Krapht", "davboecki", "theZorro266")
        val actual = roundTrip(PlayerList(ANY_ID).withDimension().apply { stringList = expected })
        assertEquals(expected, actual.stringList)
    }

    @Test
    fun `IntegerPacket round trip`() {
        val actual = roundTrip(PipeContentRequest(ANY_ID).withDimension().apply { integer = 42 })
        assertEquals(42, actual.integer)
    }

    // ── level 2: CoordinatesPacket ───────────────────────────────────────────

    @Test
    fun `CoordinatesPacket round trip`() {
        assertCoords(roundTrip(SlotFinderActivatePacket(ANY_ID).withModule()))
    }

    @Test
    fun `NBTCoordinatesPacket round trip`() {
        val tag = CompoundTag().apply { putInt("lp_test", 7); putString("who", "krapht") }
        val actual = roundTrip(SecurityStationCCIDs(ANY_ID).withCoords().apply { this.tag = tag })
        assertCoords(actual)
        assertEquals(tag, actual.tag)
    }

    @Test
    fun `StringCoordinatesPacket round trip`() {
        val actual = roundTrip(
            InvSysConSetChannelOnPipePacket(ANY_ID).withCoords().apply { string = "◘ËCanale♀ßüöä" },
        )
        assertCoords(actual)
        assertEquals("◘ËCanale♀ßüöä", actual.string)
    }

    @Test
    fun `BitSetCoordinatesPacket round trip`() {
        val flags = BitSet().apply { set(0); set(3); set(11) }
        val actual = roundTrip(FireWallFlag(ANY_ID).withCoords().apply { this.flags = flags })
        assertCoords(actual)
        assertEquals(flags, actual.flags)
    }

    @Test
    fun `ItemPacket round trip`() {
        val stack = ItemStack(Items.DIAMOND_PICKAXE, 1).also { it.damageValue = 137 }
        val actual = roundTrip(DiscContent(ANY_ID).withCoords().apply { this.stack = stack })
        assertCoords(actual)
        assertEquals(Items.DIAMOND_PICKAXE, actual.stack.item)
        assertEquals(137, actual.stack.damageValue)
    }

    // ── level 3: IntegerCoordinatesPacket ────────────────────────────────────

    @Test
    fun `IntegerCoordinatesPacket round trip`() {
        val actual = roundTrip(FluidSupplierAmount(ANY_ID).withCoords().apply { integer = 9001 })
        assertCoords(actual)
        assertEquals(9001, actual.integer)
    }

    @Test
    fun `Integer2CoordinatesPacket round trip`() {
        val actual = roundTrip(
            ItemAmountSignUpdatePacket(ANY_ID).withCoords().apply { integer = 11; integer2 = 22 },
        )
        assertCoords(actual)
        assertEquals(11, actual.integer)
        assertEquals(22, actual.integer2)
    }

    // ── level 3: ModuleCoordinatesPacket ─────────────────────────────────────

    @Test
    fun `ModuleCoordinatesPacket round trip`() {
        assertModule(roundTrip(SlotFinderActivatePacket(ANY_ID).withModule()))
    }

    @Test
    fun `InventoryModuleCoordinatesPacket round trip`() {
        val stacks = NonNullList.of(
            ItemStack.EMPTY,
            ItemStack(Items.COBBLESTONE, 5),
            ItemStack(Items.STICK, 2),
        )
        val actual = roundTrip(SendQueueContent(ANY_ID).withModule().apply { setStackList(stacks) })
        assertModule(actual)
        // A stack list goes out under STACK_MARKER and comes back as a stack list, not as the
        // identifier list the same packet can also carry.
        assertEquals(2, actual.stackList.size)
        assertEquals(Items.COBBLESTONE, actual.stackList[0].item)
        assertEquals(5, actual.stackList[0].count)
        assertEquals(Items.STICK, actual.stackList[1].item)
    }

    // ── level 4+: the deepest chains ─────────────────────────────────────────

    @Test
    fun `IntegerModuleCoordinatesPacket round trip`() {
        val actual = roundTrip(CraftingPriority(ANY_ID).withModule().apply { putInt(7) })
        assertModule(actual)
        assertEquals(7, actual.integer)
    }

    // ── the null branches, which are where asymmetries hide ──────────────────

    @Test
    fun `ModuleCoordinatesPacket with no module type round trips`() {
        // The module block is written behind a boolean; the unset branch is the one that
        // silently desyncs if the read side ever stops mirroring it.
        val actual = roundTrip(SlotFinderActivatePacket(ANY_ID).withCoords())
        assertCoords(actual)
        assertEquals(null, actual.type)
    }

    @Test
    fun `NBTCoordinatesPacket with a null tag round trips`() {
        val actual = roundTrip(SecurityStationCCIDs(ANY_ID).withCoords().apply { tag = null })
        assertCoords(actual)
        assertEquals(null, actual.tag)
    }
}
