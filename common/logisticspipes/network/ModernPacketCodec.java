package logisticspipes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataIOWrapper;

/**
 * Bridges LP's imperative {@code readData}/{@code writeData} onto vanilla's {@link StreamCodec}.
 *
 * <p>Those two methods already <em>are</em> a codec: they just happen to be imperative and to
 * compose by inheritance rather than by construction. Wrapping them means every LP packet has a
 * real {@code StreamCodec} today, without touching any of the ~150 packet classes, and without
 * changing a single byte of what is sent.
 *
 * <p>This is deliberately a stepping stone. Packets migrate off {@link #body} one at a time, each
 * one replacing its adapter with a composed codec of its own; when the last one goes, so does
 * {@code LPDataIO}. Until then both forms coexist, so the migration can stop at any commit.
 */
public final class ModernPacketCodec {

    private ModernPacketCodec() {
    }

    /**
     * The packet's own payload, without the id or the debug id: what
     * {@link ModernPacket#writeData} writes and {@link ModernPacket#readData} reads back.
     *
     * <p>Decoding starts from {@link ModernPacket#template()} because LP packets are mutable and
     * filled in place; a codec that replaces this one for a given packet is free to build the
     * value directly instead.
     *
     * @param template the registered prototype for this packet type
     */
    public static <P extends ModernPacket> StreamCodec<RegistryFriendlyByteBuf, P> body(P template) {
        return StreamCodec.of(
                (buf, packet) -> LPDataIOWrapper.writeData(buf, buf.registryAccess(), packet::writeData),
                buf -> {
                    @SuppressWarnings("unchecked")
                    final P packet = (P) template.template();
                    LPDataIOWrapper.provideData(buf, buf.registryAccess(), packet::readData);
                    return packet;
                });
    }

}
