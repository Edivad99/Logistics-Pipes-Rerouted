package logisticspipes.network.to_server.channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.channel.ChannelSelectPopupMessage;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.proxy.SimpleServiceLocator;

/**
 * Asks for the channels this player may put a connector on; answered with
 * {@link ChannelSelectPopupMessage}.
 */
public record RequestChannelSelectMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestChannelSelectMessage> TYPE =
            new Type<>(LPConstants.rl("request_channel_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestChannelSelectMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestChannelSelectMessage::pos,
                    RequestChannelSelectMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestChannelSelectMessage message, IPayloadContext context) {
        final PipeItemsInvSysConnector pipe =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, PipeItemsInvSysConnector.class);
        if (pipe == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final IChannelManager manager =
                SimpleServiceLocator.channelManagerProvider.getChannelManager(player.level());
        PacketDistributor.sendToPlayer(player,
                new ChannelSelectPopupMessage(message.pos, manager.getAllowedChannels(player)));
    }
}
