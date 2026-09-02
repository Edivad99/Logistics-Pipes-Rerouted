package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.RoutingLasersMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.RoutingLaserBuilder;

/**
 * Asks a pipe to show where it can route to.
 *
 * <p>Answering also forces a network-wide LSA update, which is the only way a player has of
 * triggering one.
 */
public record RequestRoutingLasersMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestRoutingLasersMessage> TYPE =
            new Type<>(LPConstants.rl("request_routing_lasers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRoutingLasersMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestRoutingLasersMessage::pos,
                    RequestRoutingLasersMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestRoutingLasersMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, CoreRoutedPipe.class);
        if (pipe != null && context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new RoutingLasersMessage(RoutingLaserBuilder.buildFor(pipe)));
        }
    }
}
