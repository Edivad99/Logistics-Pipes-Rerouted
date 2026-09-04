package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.Unpooled;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.renderer.state.PipeRenderState;

/**
 * Everything the client needs to draw a pipe: how it connects, which pipe it is, and whatever the
 * pipe itself keeps client side.
 *
 * <p>The last block stays opaque, and has to. Its shape depends on which pipe wrote it, and the
 * pipe is only known once the block entity has been found -- which happens on the main thread,
 * long after this has been decoded off the network one.
 *
 * @param stateId counts up per pipe, so a state that overtook a newer one on the way is dropped
 */
public record PipeStateMessage(
        BlockPos pos,
        PipeRenderState.Wire renderState,
        String pipeIdName,
        byte[] pipeState,
        int stateId
) implements CustomPacketPayload {

    public static final Type<PipeStateMessage> TYPE = new Type<>(LPConstants.rl("pipe_state"));

    public static final StreamCodec<FriendlyByteBuf, PipeStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeStateMessage::pos,
                    PipeRenderState.Wire.STREAM_CODEC.cast(), PipeStateMessage::renderState,
                    ByteBufCodecs.STRING_UTF8, PipeStateMessage::pipeIdName,
                    ByteBufCodecs.BYTE_ARRAY, PipeStateMessage::pipeState,
                    ByteBufCodecs.VAR_INT, PipeStateMessage::stateId,
                    PipeStateMessage::new);

    /** Snapshots the pipe's current client state. Server side. */
    public static PipeStateMessage of(LogisticsTileGenericPipe container) {
        final FriendlyByteBuf pipeBuffer = new FriendlyByteBuf(Unpooled.buffer());
        if (container.pipe != null) {
            container.pipe.writeState(pipeBuffer);
        }
        final byte[] pipeState = new byte[pipeBuffer.readableBytes()];
        pipeBuffer.readBytes(pipeState);
        return new PipeStateMessage(
                container.getBlockPos(),
                container.renderState.snapshot(),
                container.coreState.pipeIdName == null ? "" : container.coreState.pipeIdName,
                pipeState,
                container.statePacketId++);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeStateMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container != null) {
            message.applyTo(container);
        }
    }

    /** Reads the state into a pipe, unless it already has a newer one. */
    public void applyTo(LogisticsTileGenericPipe container) {
        if (container.statePacketId > stateId) {
            return;
        }
        container.renderState.apply(renderState);
        container.coreState.pipeIdName = pipeIdName;
        container.afterStateUpdated();
        if (container.pipe != null && pipeState.length != 0) {
            container.pipe.readState(new FriendlyByteBuf(Unpooled.wrappedBuffer(pipeState)));
        }
        container.statePacketId = stateId;
    }
}
