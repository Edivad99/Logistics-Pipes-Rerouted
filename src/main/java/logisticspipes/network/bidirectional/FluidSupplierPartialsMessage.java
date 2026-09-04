package logisticspipes.network.bidirectional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Whether a fluid supplier accepts partial deliveries.
 *
 * <p>Sent by the client when the player flips the button and by the server when the GUI opens, and
 * applied the same way either time.
 *
 * <p>The two fluid supplier pipes share nothing but this setting, which is why the handler asks
 * about both rather than resolving to a common type.
 */
public record FluidSupplierPartialsMessage(BlockPos pos, boolean requestingPartials)
        implements CustomPacketPayload {

    public static final Type<FluidSupplierPartialsMessage> TYPE =
            new Type<>(LPConstants.rl("fluid_supplier_partials"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSupplierPartialsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FluidSupplierPartialsMessage::pos,
                    ByteBufCodecs.BOOL, FluidSupplierPartialsMessage::requestingPartials,
                    FluidSupplierPartialsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidSupplierPartialsMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null) {
            return;
        }
        if (be.pipe instanceof PipeItemsFluidSupplier pipe) {
            pipe.setRequestingPartials(message.requestingPartials);
        } else if (be.pipe instanceof PipeFluidSupplierMk2 pipe) {
            pipe.setRequestingPartials(message.requestingPartials);
        }
    }
}
