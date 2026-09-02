package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A pipe has finished loading on the client, which cannot tell from the block state what signs it
 * carries.
 */
public record RequestPipeSignsMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestPipeSignsMessage> TYPE = new Type<>(LPConstants.rl("request_pipe_signs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPipeSignsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestPipeSignsMessage::pos,
                    RequestPipeSignsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPipeSignsMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof CoreRoutedPipe pipe) {
            pipe.sendSignData(context.player(), false);
        }
    }
}
