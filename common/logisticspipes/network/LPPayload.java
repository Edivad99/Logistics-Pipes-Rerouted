package logisticspipes.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import logisticspipes.network.abstractpackets.ModernPacket;

/**
 * Carries one {@link ModernPacket} as a named vanilla payload.
 *
 * <p>Replaces the single multiplexed channel LP used to share between every packet type. There,
 * a packet was identified by a {@code short} whose value was its index in the alphabetically
 * sorted list of {@code ModernPacket} subclasses found by scanning the classpath -- which made
 * the scan order part of the protocol, and any class that failed to load on one side a silent
 * shift of every id after it. Here the identity is {@link #type}, a name, resolved
 * independently on each side.
 *
 * @param packet the decoded packet; still mutable, still the one that carries the behaviour
 * @param type   the registered payload type for {@code packet}'s class
 */
public record LPPayload(ModernPacket packet, Type<LPPayload> type) implements CustomPacketPayload {
    // The record's own type() accessor already satisfies CustomPacketPayload.type(): its
    // Type<LPPayload> is a Type<? extends CustomPacketPayload>. Declaring the override
    // explicitly would widen the return type, which a record component accessor may not do.
}
