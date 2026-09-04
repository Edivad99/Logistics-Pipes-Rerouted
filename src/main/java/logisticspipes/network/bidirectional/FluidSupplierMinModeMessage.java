package logisticspipes.network.bidirectional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidSupplierMk2.MinMode;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The smallest amount a fluid supplier will request at a time.
 *
 * <p>Sent by the client when the player cycles the button and by the server when the GUI opens, and
 * applied the same way either time.
 */
public record FluidSupplierMinModeMessage(BlockPos pos, MinMode mode) implements CustomPacketPayload {

    public static final Type<FluidSupplierMinModeMessage> TYPE =
            new Type<>(LPConstants.rl("fluid_supplier_min_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSupplierMinModeMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FluidSupplierMinModeMessage::pos,
                    NeoForgeStreamCodecs.enumCodec(MinMode.class), FluidSupplierMinModeMessage::mode,
                    FluidSupplierMinModeMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidSupplierMinModeMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeFluidSupplierMk2 pipe) {
            pipe.setMinMode(message.mode);
        }
    }
}
