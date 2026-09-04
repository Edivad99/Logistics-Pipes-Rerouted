package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What a pipe is holding back because it has nowhere to send it.
 *
 * <p>Only the stacks travel. The server also keeps a timeout and the travelling item each entry
 * came from, but the client draws the buffer and nothing else.
 */
public record PipeItemBufferMessage(BlockPos pos, List<ItemIdentifierStack> contents)
        implements CustomPacketPayload {

    public static final Type<PipeItemBufferMessage> TYPE = new Type<>(LPConstants.rl("pipe_item_buffer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeItemBufferMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeItemBufferMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()), PipeItemBufferMessage::contents,
                    PipeItemBufferMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeItemBufferMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe != null && be.pipe.transport != null) {
            be.pipe.transport.setClientItemBuffer(message.contents);
        }
    }
}
