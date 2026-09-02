package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.util.LPDataIOWrapper;

/**
 * Everything the client needs to draw a pipe: how it connects, which pipe it is, and whatever the
 * pipe itself keeps client side.
 *
 * <p>The three blocks stay opaque here on purpose. They are not decoded into new objects but read
 * back into the ones the client already has, and the last of them is written by the pipe class
 * itself, so its shape depends on which pipe this is. They are the last users of the old data
 * stream, and go when {@code IClientState} does.
 *
 * @param stateId counts up per pipe, so a state that overtook a newer one on the way is dropped
 */
public record PipeStateMessage(
        BlockPos pos,
        byte[] renderState,
        byte[] coreState,
        byte[] pipeState,
        int stateId
) implements CustomPacketPayload {

    public static final Type<PipeStateMessage> TYPE = new Type<>(LPConstants.rl("pipe_state"));

    public static final StreamCodec<FriendlyByteBuf, PipeStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeStateMessage::pos,
                    ByteBufCodecs.BYTE_ARRAY, PipeStateMessage::renderState,
                    ByteBufCodecs.BYTE_ARRAY, PipeStateMessage::coreState,
                    ByteBufCodecs.BYTE_ARRAY, PipeStateMessage::pipeState,
                    ByteBufCodecs.VAR_INT, PipeStateMessage::stateId,
                    PipeStateMessage::new);

    /** Snapshots the pipe's current client state. Server side. */
    public static PipeStateMessage of(LogisticsTileGenericPipe container) {
        return new PipeStateMessage(
                container.getBlockPos(),
                LPDataIOWrapper.collectData(container.renderState::writeData),
                LPDataIOWrapper.collectData(container.coreState::writeData),
                container.pipe == null ? new byte[0] : LPDataIOWrapper.collectData(container.pipe::writeData),
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
        LPDataIOWrapper.provideData(renderState, container.renderState::readData);
        LPDataIOWrapper.provideData(coreState, container.coreState::readData);
        container.afterStateUpdated();
        if (container.pipe != null && pipeState.length != 0) {
            LPDataIOWrapper.provideData(pipeState, container.pipe::readData);
        }
        container.statePacketId = stateId;
    }
}
