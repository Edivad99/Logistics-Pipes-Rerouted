package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.transport.PipeFluidTransportLogistics;

/**
 * How much of what a fluid pipe is holding on each of its sides, so the client can draw it.
 *
 * <p>One entry per side, empty where there is nothing. The old format wrote a {@code BitSet} of
 * which sides were occupied and then only those stacks; {@code OPTIONAL_STREAM_CODEC} already
 * spends one byte on an empty stack, so the bitset bought nothing.
 */
public record PipeFluidUpdateMessage(BlockPos pos, List<FluidStack> sides) implements CustomPacketPayload {

    public static final Type<PipeFluidUpdateMessage> TYPE = new Type<>(LPConstants.rl("pipe_fluid_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeFluidUpdateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeFluidUpdateMessage::pos,
                    FluidStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), PipeFluidUpdateMessage::sides,
                    PipeFluidUpdateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeFluidUpdateMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || be.pipe == null
                || !(be.pipe.transport instanceof PipeFluidTransportLogistics transport)) {
            return;
        }
        final FluidStack[] sides = new FluidStack[Direction.values().length];
        for (int i = 0; i < sides.length; i++) {
            // The render cache uses null, not an empty stack, for a side with nothing on it.
            final FluidStack side = i < message.sides.size() ? message.sides.get(i) : FluidStack.EMPTY;
            sides[i] = side.isEmpty() ? null : side;
        }
        transport.renderCache = sides;
    }
}
