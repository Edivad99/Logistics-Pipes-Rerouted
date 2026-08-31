package logisticspipes.network.to_server;

import java.lang.ref.WeakReference;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.PipeContentPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;

/**
 * The client wants to know what an item travelling through a pipe actually is.
 *
 * <p>Items in transit are drawn from a light-weight client-side list that carries only an id; the
 * stack itself is asked for on demand, which is what this message does.
 */
public record RequestPipeContentMessage(int travelId) implements CustomPacketPayload {

    public static final Type<RequestPipeContentMessage> TYPE =
            new Type<>(LPConstants.rl("request_pipe_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPipeContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RequestPipeContentMessage::travelId,
                    RequestPipeContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPipeContentMessage message, IPayloadContext context) {
        final WeakReference<LPTravelingItemServer> ref = LPTravelingItem.serverList.get(message.travelId);
        if (ref == null) {
            return;
        }
        final LPTravelingItemServer item = ref.get();
        if (item != null) {
            MainProxy.sendPacketToPlayer(
                    PacketHandler.getPacket(PipeContentPacket.class)
                            .setItem(item.getItemIdentifierStack())
                            .setTravelId(item.getId()),
                    context.player());
        }
    }
}
