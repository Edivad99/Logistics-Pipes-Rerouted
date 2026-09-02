package network.rs485.logisticspipes.network

import io.netty.buffer.Unpooled
import logisticspipes.request.resources.DictResource
import logisticspipes.request.resources.IResource
import logisticspipes.request.resources.ItemResource
import logisticspipes.utils.item.ItemIdentifierStack
import net.minecraft.SharedConstants
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.network.connection.ConnectionType
import network.rs485.logisticspipes.util.FuzzyFlag
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Guards the dispatched codec for [IResource].
 *
 * The three implementations are told apart by a tag, and each carries a different body, so the
 * thing worth checking is that a resource comes back as the same kind it went out as -- and that
 * the buffer is left empty, which is what catches a body that reads a field its writer never wrote.
 */
class ResourceCodecTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private fun buffer(): RegistryFriendlyByteBuf = RegistryFriendlyByteBuf(
        Unpooled.buffer(),
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
        ConnectionType.OTHER,
    )

    private fun roundTrip(resource: IResource): IResource {
        val buf = buffer()
        try {
            IResource.STREAM_CODEC.encode(buf, resource)
            val decoded = IResource.STREAM_CODEC.decode(buf)
            assertEquals(0, buf.readableBytes(), "the codec must consume exactly what it wrote")
            return decoded
        } finally {
            buf.release()
        }
    }

    private fun stack(count: Int) = ItemIdentifierStack.getFromStack(ItemStack(Items.STICK, count))

    @Test
    fun `an item resource round trips`() {
        val actual = roundTrip(ItemResource(stack(3), null))
        assertIs<ItemResource>(actual)
        assertEquals(3, actual.requestedAmount)
    }

    @Test
    fun `a dict resource with no fuzzy flags round trips`() {
        // The empty flag set is the ordinary case, and the one an encoding that reads the BitSet's
        // first byte gets wrong: an empty BitSet has no bytes at all.
        val actual = roundTrip(DictResource(stack(1), null))
        assertIs<DictResource>(actual)
        FuzzyFlag.values().forEach { assertEquals(false, actual.hasFuzzyFlag(it), it.name) }
    }

    @Test
    fun `every fuzzy flag survives the round trip`() {
        FuzzyFlag.values().forEach { flag ->
            val sent = DictResource(stack(1), null)
                .loadFromBitSet(java.util.BitSet().apply { set(flag.bit) })
            val actual = roundTrip(sent)
            assertIs<DictResource>(actual)
            FuzzyFlag.values().forEach { other ->
                assertEquals(other == flag, actual.hasFuzzyFlag(other), "$flag -> $other")
            }
        }
    }
}
