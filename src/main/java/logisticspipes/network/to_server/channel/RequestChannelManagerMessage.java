package logisticspipes.network.to_server.channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.channel.ChannelManagerPopupMessage;
import logisticspipes.proxy.SimpleServiceLocator;

/**
 * Asks for the channels this player may manage; answered with {@link ChannelManagerPopupMessage}.
 */
public record RequestChannelManagerMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestChannelManagerMessage> TYPE =
            new Type<>(LPConstants.rl("request_channel_manager"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestChannelManagerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestChannelManagerMessage::pos,
                    RequestChannelManagerMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestChannelManagerMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity station =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (station == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final IChannelManager manager =
                SimpleServiceLocator.channelManagerProvider.getChannelManager(player.level());
        PacketDistributor.sendToPlayer(player,
                new ChannelManagerPopupMessage(message.pos, manager.getAllowedChannels(player)));
    }
}
