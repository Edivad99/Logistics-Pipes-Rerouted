package logisticspipes.network.to_server.channel;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.channels.ChannelManager;

/**
 * The player removed a channel in the channel manager.
 */
public record DeleteChannelMessage(UUID channel) implements CustomPacketPayload {

    public static final Type<DeleteChannelMessage> TYPE = new Type<>(LPConstants.rl("delete_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteChannelMessage> STREAM_CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DeleteChannelMessage::channel,
            DeleteChannelMessage::new);

    public static void handle(DeleteChannelMessage message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ChannelManager.get(player.level()).removeChannel(message.channel);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
