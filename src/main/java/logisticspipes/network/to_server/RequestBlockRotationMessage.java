package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.BlockRotationMessage;

/**
 * A block that faces a direction has loaded on the client, which has no way to know which one.
 */
public record RequestBlockRotationMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestBlockRotationMessage> TYPE =
            new Type<>(LPConstants.rl("request_block_rotation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBlockRotationMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestBlockRotationMessage::pos,
                    RequestBlockRotationMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestBlockRotationMessage message, IPayloadContext context) {
        final IRotationProvider target =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, IRotationProvider.class);
        if (target != null && context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new BlockRotationMessage(message.pos, target.getRotation()));
        }
    }
}
