package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The amount a fluid supplier is set to request, after the server has clamped it.
 *
 * <p>Sent in answer to every
 * {@link logisticspipes.network.to_server.pipe.ChangeFluidSupplierAmountMessage}, which is what lets the
 * GUI send a change rather than a value.
 */
public record FluidSupplierAmountMessage(BlockPos pos, int amount) implements CustomPacketPayload {

    public static final Type<FluidSupplierAmountMessage> TYPE =
            new Type<>(LPConstants.rl("fluid_supplier_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSupplierAmountMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FluidSupplierAmountMessage::pos,
                    ByteBufCodecs.VAR_INT, FluidSupplierAmountMessage::amount,
                    FluidSupplierAmountMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidSupplierAmountMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeFluidSupplierMk2 pipe) {
            pipe.setAmount(message.amount);
        }
    }
}
