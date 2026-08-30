package logisticspipes.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static io.netty.buffer.Unpooled.buffer;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.exception.DelayPacketException;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.LPDataIOWrapper;
import logisticspipes.util.LPDataInput;
import logisticspipes.utils.StaticResolverUtil;

/**
 * Central packet registry and dispatcher for LogisticsPipes.
 *
 * All LP packets share a single {@link LPPacketPayload} channel multiplexed by a short ID.
 * Registration happens in {@link LogisticsPipes} via {@code RegisterPayloadsEvent}.
 */
public class PacketHandler {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar(LPConstants.ID).versioned("1");
            // Both directions get the handler explicitly. The three-argument playBidirectional only
            // registers the *server* one and leaves the clientbound side null, which 1.21.8 now
            // rejects outright: "Some clientbound payloads are missing client-side handlers".
            // LP multiplexes every packet over this one channel and dispatches by id inside
            // handlePayload, so the same handler is correct for both.
            registrar.playBidirectional(
                    LPPacketPayload.TYPE,
                    LPPacketPayload.STREAM_CODEC,
                    PacketHandler::handlePayload,
                    PacketHandler::handlePayload
            );
            registerClientToServer(registrar);
            registerServerToClient(registrar);
        });
    }

    private static void registerClientToServer(PayloadRegistrar registrar) {
    }

    private static void registerServerToClient(PayloadRegistrar registrar) {
    }

    private static void handlePayload(LPPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Player player = context.player();

                onPacketData(payload.getData(), player);
            } finally {
                payload.release();
            }
        });
    }

    public static final Map<Integer, StackTraceElement[]> debugMap = new HashMap<>();
    public static List<ModernPacket> packetlist;
    public static Map<Class<? extends ModernPacket>, ModernPacket> packetmap;
    private static int packetDebugID = 1;

    // ── Registration ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T extends ModernPacket> T getPacket(Class<T> clazz) {
        T packet = (T) PacketHandler.packetmap.get(clazz).template();
        if (LogisticsPipes.isDEBUG() && MainProxy.proxy.getSide().equals("Client")) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            synchronized (PacketHandler.debugMap) {
                int id = PacketHandler.packetDebugID++;
                PacketHandler.debugMap.put(id, trace);
                packet.setDebugId(id);
            }
        }
        return packet;
    }

    /** Enumerates all ModernPacket subclasses, assigns IDs, and populates packetlist/packetmap. */
    public static void initialize() {
        Set<Class<? extends ModernPacket>> classes = StaticResolverUtil.findClassesByType(ModernPacket.class);
        loadPackets(classes);
        if (PacketHandler.packetmap.isEmpty()) {
            throw new RuntimeException("Cannot load Packet Classes");
        }
    }

    private static void loadPackets(Set<Class<? extends ModernPacket>> classesIn) {
        List<Class<? extends ModernPacket>> classes = classesIn.stream()
                .sorted(Comparator.comparing(Class::getCanonicalName))
                .collect(Collectors.toList());

        // Packet IDs are the index in the sorted class list, so they are IDENTICAL on the client
        // and the dedicated server even if a packet fails to construct on one side (e.g. a class
        // the RuntimeDistCleaner refuses to link). A success-counter would shift every later ID
        // and desync the protocol. Failed slots stay null; the dispatch path guards against that.
        PacketHandler.packetlist = new ArrayList<>(java.util.Collections.nCopies(classes.size(), (ModernPacket) null));
        PacketHandler.packetmap = new HashMap<>(classes.size());

        for (int id = 0; id < classes.size(); id++) {
            Class<? extends ModernPacket> cls = classes.get(id);
            try {
                final ModernPacket instance = cls.getConstructor(int.class).newInstance(id);
                PacketHandler.packetlist.set(id, instance);
                PacketHandler.packetmap.put(cls, instance);
            } catch (Throwable t) {
                LogisticsPipes.LOG.error("Failed to load packet (id " + id + ") " + cls.getName(), t);
            }
        }
    }

    // ── Binary serialization ─────────────────────────────────────────────────

    /**
     * Writes a ModernPacket into a raw ByteBuf (short id + int debugId + LP body).
     * Used both for network sending and for NBT embedding.
     */
    public static void fillByteBuf(ModernPacket msg, ByteBuf buffer) {
        buffer.writeShort(msg.getId());
        buffer.writeInt(msg.getDebugId());
        LPDataIOWrapper.writeData(buffer, msg::writeData);
    }

    public static void addPacketToNBT(ModernPacket packet, CompoundTag nbt) {
        ByteBuf dataBuffer = buffer();
        PacketHandler.fillByteBuf(packet, dataBuffer);

        byte[] data = new byte[dataBuffer.readableBytes()];
        dataBuffer.getBytes(0, data);
        dataBuffer.release();

        nbt.putByteArray("LogisticsPipes:PacketData", data);
    }

    /**
     * Reads back the packet {@link #addPacketToNBT} embedded in an update tag and queues it.
     *
     * <p>Takes a {@link ValueInput} because that is what {@code handleUpdateTag} hands out since
     * 1.21.6. It has no byte-array accessor, so the payload comes back through
     * {@code Codec.BYTE_BUFFER}, which NbtOps maps onto the same ByteArrayTag the writer produces.
     * The key is no longer removed afterwards -- a ValueInput is read-only, and leaving it costs
     * nothing since the block entity ignores unknown keys.</p>
     */
    public static void queuePacketFromUpdateTag(ValueInput input) {
        byte[] data = input.read("LogisticsPipes:PacketData", Codec.BYTE_BUFFER)
            .map(buffer -> {
                byte[] copy = new byte[buffer.remaining()];
                buffer.duplicate().get(copy);
                return copy;
            })
            .orElse(new byte[0]);
        if (data.length > 0) {
            LPDataIOWrapper.provideData(data, Minecraft.getInstance().getConnection() != null ? Minecraft.getInstance().getConnection().registryAccess() : null, dataInput -> {
                final int packetID = dataInput.readShort();
                final ModernPacket packet = PacketHandler.templateForId(packetID);
                packet.setDebugId(dataInput.readInt());
                packet.readData(dataInput);
                Player localPlayer = Minecraft.getInstance().player;
                SimpleServiceLocator.clientBufferHandler.queuePacket(packet, Objects.requireNonNull(localPlayer));
            });
        }
    }

    // ── Sending ──────────────────────────────────────────────────────────────

    private static LPPacketPayload buildPayload(ModernPacket msg) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        fillByteBuf(msg, buf);
        return LPPacketPayload.of(buf);
    }

    public static LPPacketPayload buildPayloadPublic(ModernPacket msg) {
        return buildPayload(msg);
    }

    /** Sends a packet from the client to the server. Must only be called client-side. */
    public static void sendToServer(ModernPacket msg) {
        ClientPacketDistributor.sendToServer(buildPayload(msg));
    }

    /** Sends a packet from the server to a specific player. Must only be called server-side. */
    public static void sendToPlayer(ModernPacket msg, Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            LogisticsPipes.LOG.warn("sendToPlayer: player is not a ServerPlayer, skipping");
            return;
        }

        PacketDistributor.sendToPlayer(
                sp,
                buildPayload(msg)
        );
    }

    /** Sends a packet to every connected player. Must only be called server-side. */
    public static void sendToAll(ModernPacket msg) {
        PacketDistributor.sendToAllPlayers(
                buildPayload(msg)
        );
    }

    /** Resolves a fresh packet template for a received id, guarding the null gaps that
     *  {@link #loadPackets} leaves for packets unavailable on this side. A non-null result is
     *  expected in normal play (IDs are index-based and symmetric); a gap means a client/server
     *  packet-table mismatch. */
    private static ModernPacket templateForId(int packetID) {
        ModernPacket tmpl = (packetID >= 0 && packetID < packetlist.size()) ? packetlist.get(packetID) : null;
        if (tmpl == null) {
            throw new IllegalStateException("Received LP packet id " + packetID
                    + " not registered on this side (client/server packet-table mismatch)");
        }
        return tmpl.template();
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    /** Decodes and dispatches a raw LP packet from a FriendlyByteBuf. */
    public static void onPacketData(final FriendlyByteBuf data, final Player player) {
        LPDataIOWrapper.provideData(data, player.registryAccess(), input -> {
            final int packetID = input.readShort();
            final ModernPacket packet = PacketHandler.templateForId(packetID);
            packet.setDebugId(input.readInt());
            packet.readData(input);
            onPacketData(packet, player);
        });
    }

    /** Decodes a raw LP packet from an LPDataInput (used by NBT-embedded packets). */
    public static void onPacketData(final LPDataInput data, final Player player) {
        final int packetID = data.readShort();
        final ModernPacket packet = PacketHandler.packetlist.get(packetID).template();
        packet.setDebugId(data.readInt());
        packet.readData(data);
        onPacketData(packet, player);
    }

    /** Processes a fully-decoded ModernPacket on the correct thread. */
    public static void onPacketData(ModernPacket packet, final Player player) {
        try {
            packet.processPacket(player);
            if (LogisticsPipes.isDEBUG()) {
                PacketHandler.debugMap.remove(packet.getDebugId());
            }
        } catch (DelayPacketException e) {
            if (packet.retry() && MainProxy.isClient(player.level())) {
                SimpleServiceLocator.clientBufferHandler.queuePacket(packet, player);
            } else if (LogisticsPipes.isDEBUG()) {
                LogisticsPipes.LOG.error(packet.getClass().getName());
                LogisticsPipes.LOG.error(packet.toString());
                LogisticsPipes.LOG.error("Packet handling error", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
