package logisticspipes.network.to_server.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.InvSysConContentMessage;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The inventory system connector's screen wants to know what is still on its way.
 */
public record RequestInvSysConContentMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestInvSysConContentMessage> TYPE =
            new Type<>(LPConstants.rl("request_inv_sys_con_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestInvSysConContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestInvSysConContentMessage::pos,
                    RequestInvSysConContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestInvSysConContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeItemsInvSysConnector pipe
                && context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new InvSysConContentMessage(List.copyOf(pipe.getExpectedItems())));
        }
    }
}
