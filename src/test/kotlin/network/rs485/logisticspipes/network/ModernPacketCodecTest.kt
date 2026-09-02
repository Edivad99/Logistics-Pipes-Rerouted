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
import logisticspipes.network.ModernPacketCodec
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.connection.ConnectionType
import logisticspipes.util.LPDataIOWrapper
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Guards the adapter codec that bridges LP's `readData`/`writeData` onto vanilla's `StreamCodec`.
 *
 * The point of the adapter is that it is a pure re-housing: the same bytes, produced by the same
 * code, reached through a different interface. The byte-identity test below is what makes that
 * claim checkable rather than merely intended -- without it, "nothing changed" is a hope.
 */
class ModernPacketCodecTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            TestBootstrap.boot()
        }

        private const val PACKET_ID = 0
    }

    private fun registries(): RegistryAccess =
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    private fun buffer(): RegistryFriendlyByteBuf =
        RegistryFriendlyByteBuf(Unpooled.buffer(), registries(), ConnectionType.OTHER)

    /** A packet with a coordinate and a value, filled in at every level of its chain. */
    private fun samplePacket(): SampleCoordsPacket =
        SampleCoordsPacket(PACKET_ID).apply {
            setDimension(Identifier.parse("logisticspipes:test_dimension"))
            posX = 12
            posY = -34
            posZ = 5678
            debugId = 77
        }

    private fun bytesOf(buf: RegistryFriendlyByteBuf): ByteArray =
        ByteArray(buf.readableBytes()).also { buf.getBytes(0, it) }

    @Test
    fun `body codec round trips a packet`() {
        val expected = samplePacket()
        val buf = buffer()
        try {
            ModernPacketCodec.body(expected).encode(buf, expected)
            val actual = ModernPacketCodec.body(expected).decode(buf)

            assertEquals(12, actual.posX)
            assertEquals(-34, actual.posY)
            assertEquals(5678, actual.posZ)
            assertEquals(Identifier.parse("logisticspipes:test_dimension"), actual.dimension)
            assertEquals(0, buf.readableBytes(), "the body codec must consume exactly what it wrote")
        } finally {
            buf.release()
        }
    }

    @Test
    fun `body codec writes exactly what writeData writes`() {
        val packet = samplePacket()
        val legacy = LPDataIOWrapper.collectData(registries()) { output -> packet.writeData(output) }

        val buf = buffer()
        try {
            ModernPacketCodec.body(packet).encode(buf, packet)
            assertContentEquals(legacy, bytesOf(buf), "the adapter must not change the body bytes")
        } finally {
            buf.release()
        }
    }

}
