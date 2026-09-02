package logisticspipes.network.to_server.channel;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.proxy.SimpleServiceLocator;

/** The player removed a channel in the channel manager. */
public record DeleteChannelMessage(UUID channel) implements CustomPacketPayload {

    public static final Type<DeleteChannelMessage> TYPE = new Type<>(LPConstants.rl("delete_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteChannelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, DeleteChannelMessage::channel,
                    DeleteChannelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteChannelMessage message, IPayloadContext context) {
        SimpleServiceLocator.channelManagerProvider.getChannelManager(context.player().level())
                .removeChannel(message.channel);
    }
}
