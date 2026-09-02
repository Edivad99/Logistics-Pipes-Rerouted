package network.rs485.logisticspipes.network

import logisticspipes.network.abstractpackets.CoordinatesPacket
import logisticspipes.network.abstractpackets.ModernPacket
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket
import logisticspipes.util.LPDataInput
import logisticspipes.util.LPDataOutput
import net.minecraft.world.entity.player.Player

/**
 * Stand-in packets, one per level of the `ModernPacket` chain.
 *
 * The tests around the legacy packet system used real packets as samples; as those became payload
 * records, the samples went with them. What is under test is the chain and the table, not any
 * particular leaf, so the samples live here.
 *
 * Top level on purpose: the payload name is derived from the class name, and a nested class'
 * `$` is not a legal [net.minecraft.resources.Identifier] path.
 */
class SampleModernPacket(id: Int) : ModernPacket(id) {
    override fun template(): ModernPacket = SampleModernPacket(id)
    override fun processPacket(player: Player) = Unit
}

class SampleCoordsPacket(id: Int) : CoordinatesPacket(id) {
    override fun template(): ModernPacket = SampleCoordsPacket(id)
    override fun processPacket(player: Player) = Unit
}

class SampleModulePacket(id: Int) : ModuleCoordinatesPacket(id) {
    override fun template(): ModernPacket = SampleModulePacket(id)
    override fun processPacket(player: Player) = Unit
}

/** Overrides both halves without chaining, the way a packet with no payload of its own does. */
class SampleSilentPacket(id: Int) : ModernPacket(id) {
    override fun template(): ModernPacket = SampleSilentPacket(id)
    override fun processPacket(player: Player) = Unit
    override fun writeData(output: LPDataOutput) = Unit
    override fun readData(input: LPDataInput) = Unit
}
