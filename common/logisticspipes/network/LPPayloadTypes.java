package logisticspipes.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.network.abstractpackets.ModernPacket;

/**
 * One registered payload type per packet class, keyed by name instead of by position.
 *
 * <p>The name is derived from the class's own fully qualified name, so it is stable across
 * builds, independent of how many packet classes exist and of the order they are discovered in.
 * Adding, removing or renaming a packet no longer shifts the identity of any other packet.
 */
public final class LPPayloadTypes {

    private static Map<Identifier, Entry> byName = Collections.emptyMap();
    private static Map<Class<? extends ModernPacket>, Entry> byClass = Collections.emptyMap();

    private LPPayloadTypes() {
    }

    /**
     * The registered type, template and codec for one packet class.
     */
    public record Entry(
            Identifier name,
            CustomPacketPayload.Type<LPPayload> type,
            ModernPacket template,
            StreamCodec<RegistryFriendlyByteBuf, LPPayload> codec
    ) {
    }

    /**
     * Derives the payload name for a packet class.
     *
     * <p>The fully qualified name, lowercased and with dots turned into path separators: unique
     * by construction, and legible in a packet dump.
     */
    public static Identifier nameOf(Class<? extends ModernPacket> packetClass) {
        return LPConstants.rl(packetClass.getName().toLowerCase(Locale.ROOT).replace('.', '/'));
    }

    /**
     * Builds the type table from the templates {@code PacketHandler} discovered.
     *
     * <p>Null entries are the slots of packet classes that failed to construct on this side; they
     * are skipped rather than registered, which is now harmless -- with named types, a packet
     * missing on one side no longer disturbs any other.
     */
    public static void build(Iterable<@Nullable ModernPacket> templates) {
        final Map<Identifier, Entry> names = new HashMap<>();
        final Map<Class<? extends ModernPacket>, Entry> classes = new HashMap<>();
        for (ModernPacket template : templates) {
            if (template == null) {
                continue;
            }
            final Class<? extends ModernPacket> packetClass = template.getClass();
            final Identifier name = nameOf(packetClass);
            final CustomPacketPayload.Type<LPPayload> type = new CustomPacketPayload.Type<>(name);
            final Entry entry = new Entry(name, type, template, codecFor(template, type));
            if (names.put(name, entry) != null) {
                throw new IllegalStateException("Two LP packets claim the payload name " + name);
            }
            classes.put(packetClass, entry);
        }
        byName = Map.copyOf(names);
        byClass = Map.copyOf(classes);
    }

    /**
     * The debug id followed by the packet body. The packet id is gone: the payload type is the
     * discriminator now.
     */
    private static StreamCodec<RegistryFriendlyByteBuf, LPPayload> codecFor(
            ModernPacket template,
            CustomPacketPayload.Type<LPPayload> type
    ) {
        return StreamCodec.composite(
                ByteBufCodecs.INT,
                payload -> payload.packet().getDebugId(),
                ModernPacketCodec.body(template),
                LPPayload::packet,
                (debugId, packet) -> {
                    packet.setDebugId(debugId);
                    return new LPPayload(packet, type);
                });
    }

    public static Iterable<Entry> all() {
        return byName.values();
    }

    /**
     * Wraps {@code packet} in the payload registered for its class.
     *
     * @throws IllegalStateException when the class was never registered, which means the packet
     *                               table was not built or the class is new to this side
     */
    public static LPPayload payloadFor(ModernPacket packet) {
        return new LPPayload(packet, entryFor(packet).type());
    }

    public static Entry entryFor(ModernPacket packet) {
        final Entry entry = byClass.get(packet.getClass());
        if (entry == null) {
            throw new IllegalStateException("No payload type registered for " + packet.getClass().getName());
        }
        return entry;
    }

    public static @Nullable Entry entryFor(Identifier name) {
        return byName.get(name);
    }
}
