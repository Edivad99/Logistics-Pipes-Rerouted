package logisticspipes.network.bidirectional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;

/**
 * A batch of legacy packets, compressed into one blob.
 *
 * <p>Carries the last of the {@code ModernPacket} channel: the buffer threads batch what is still
 * queued, compress it and send it here. It goes when they do.
 */
public record BufferedPacketsMessage(byte[] compressed) implements CustomPacketPayload {

    public static final Type<BufferedPacketsMessage> TYPE = new Type<>(LPConstants.rl("buffered_packets"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BufferedPacketsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE_ARRAY, BufferedPacketsMessage::compressed,
                    BufferedPacketsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BufferedPacketsMessage message, IPayloadContext context) {
        if (MainProxy.isClient(context.player().level())) {
            SimpleServiceLocator.clientBufferHandler.handlePacket(message.compressed);
        } else {
            SimpleServiceLocator.serverBufferHandler.handlePacket(message.compressed, context.player());
        }
    }
}
