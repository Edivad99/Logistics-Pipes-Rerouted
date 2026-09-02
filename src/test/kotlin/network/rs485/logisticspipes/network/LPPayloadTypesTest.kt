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

import network.rs485.logisticspipes.TestBootstrap
import io.netty.buffer.Unpooled
import logisticspipes.network.LPPayloadTypes
import logisticspipes.network.PacketHandler
import logisticspipes.network.abstractpackets.ModernPacket
import logisticspipes.network.packets.ActivateNBTDebug
import logisticspipes.network.packets.cpipe.CraftingPipeOpenConnectedGuiPacket
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.connection.ConnectionType
import org.junit.jupiter.api.BeforeAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Covers the payload table that replaced the positional packet id.
 *
 * The property that matters is that a packet's identity no longer depends on how many other
 * packet classes exist or on the order a classpath scan happened to return them in. The tests
 * below check that directly: same class, same name, whatever else is in the table.
 */
class LPPayloadTypesTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            TestBootstrap.boot()
        }
    }

    private var previous: List<ModernPacket?>? = null

    @BeforeTest
    fun rememberTable() {
        previous = PacketHandler.packetlist
    }

    @AfterTest
    fun restoreTable() {
        PacketHandler.packetlist = previous
    }

    private fun buffer(): RegistryFriendlyByteBuf =
        RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
            ConnectionType.OTHER,
        )

    private fun buildTableOf(vararg templates: ModernPacket) {
        val table = templates.toList()
        PacketHandler.packetlist = table
        LPPayloadTypes.build(table)
    }

    @Test
    fun `a packet name is derived from its class, not its position`() {
        buildTableOf(CraftingPipeOpenConnectedGuiPacket(0), ActivateNBTDebug(1))
        val first = LPPayloadTypes.entryFor(CraftingPipeOpenConnectedGuiPacket(0)).name()

        // Same class, different id, different neighbours in the table.
        buildTableOf(ActivateNBTDebug(0), CraftingPipeOpenConnectedGuiPacket(7))
        val second = LPPayloadTypes.entryFor(CraftingPipeOpenConnectedGuiPacket(7)).name()

        assertEquals(first, second, "the payload name must not depend on the packet table")
        assertEquals(
            Identifier.parse(
                "logisticspipes:logisticspipes/network/packets/cpipe/craftingpipeopenconnectedguipacket",
            ),
            first,
        )
    }

    @Test
    fun `different packets get different names`() {
        buildTableOf(CraftingPipeOpenConnectedGuiPacket(0), ActivateNBTDebug(1))
        assertNotEquals(
            LPPayloadTypes.entryFor(CraftingPipeOpenConnectedGuiPacket(0)).name(),
            LPPayloadTypes.entryFor(ActivateNBTDebug(1)).name(),
        )
    }

    @Test
    fun `null slots in the packet table are skipped`() {
        // A class that failed to construct on this side leaves a null. It used to be load-bearing:
        // the slot had to stay so every later id kept its value. Now it is simply skipped.
        val table = listOf(null, CraftingPipeOpenConnectedGuiPacket(1), null)
        PacketHandler.packetlist = table
        LPPayloadTypes.build(table)

        assertEquals(1, LPPayloadTypes.all().count())
        assertNull(LPPayloadTypes.entryFor(Identifier.parse("logisticspipes:nope")))
    }

    @Test
    fun `payload codec round trips a packet with its debug id`() {
        buildTableOf(CraftingPipeOpenConnectedGuiPacket(0))
        val packet = CraftingPipeOpenConnectedGuiPacket(0).apply {
            setDimension(Identifier.parse("logisticspipes:test_dimension"))
            posX = 1
            posY = 2
            posZ = 3
            debugId = 99
        }

        val entry = LPPayloadTypes.entryFor(packet)
        val buf = buffer()
        try {
            entry.codec().encode(buf, LPPayloadTypes.payloadFor(packet))
            val actual = entry.codec().decode(buf).packet() as CraftingPipeOpenConnectedGuiPacket

            assertEquals(99, actual.debugId)
            assertEquals(3, actual.posZ)
            assertEquals(0, buf.readableBytes(), "the payload codec must consume exactly what it wrote")
        } finally {
            buf.release()
        }
    }

    @Test
    fun `payloadFor reports a packet that was never registered`() {
        buildTableOf(CraftingPipeOpenConnectedGuiPacket(0))
        val error = runCatching { LPPayloadTypes.payloadFor(ActivateNBTDebug(1)) }.exceptionOrNull()
        assertEquals(IllegalStateException::class, error!!::class)
    }
}
