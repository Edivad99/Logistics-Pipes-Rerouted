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
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Which sign, if any, is stuck to each of a pipe's six sides.
 *
 * <p>An entry is an index into the registered sign types, or -1 for a bare side. The signs
 * themselves then send their own contents, one message each.
 */
public record PipeSignTypesMessage(BlockPos pos, List<Integer> types) implements CustomPacketPayload {

    public static final Type<PipeSignTypesMessage> TYPE = new Type<>(LPConstants.rl("pipe_sign_types"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeSignTypesMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeSignTypesMessage::pos,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), PipeSignTypesMessage::types,
                    PipeSignTypesMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeSignTypesMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.isInitialized() && be.pipe instanceof CoreRoutedPipe pipe) {
            pipe.handleSignPacket(message.types);
        }
    }
}
