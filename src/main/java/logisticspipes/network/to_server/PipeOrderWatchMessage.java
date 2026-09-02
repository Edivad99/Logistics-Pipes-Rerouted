package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The pipe controller's order list opened or closed, so the pipe knows whether to keep sending it.
 */
public record PipeOrderWatchMessage(BlockPos pos, boolean watching) implements CustomPacketPayload {

    public static final Type<PipeOrderWatchMessage> TYPE = new Type<>(LPConstants.rl("pipe_order_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeOrderWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeOrderWatchMessage::pos,
                    ByteBufCodecs.BOOL, PipeOrderWatchMessage::watching,
                    PipeOrderWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeOrderWatchMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof CoreRoutedPipe pipe)) {
            return;
        }
        if (message.watching) {
            pipe.getOrderManager().startWatching(context.player());
        } else {
            pipe.getOrderManager().stopWatching(context.player());
        }
    }
}
