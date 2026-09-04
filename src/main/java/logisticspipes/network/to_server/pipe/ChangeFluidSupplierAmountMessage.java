package logisticspipes.network.to_server.pipe;

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
 * The player pressed one of the amount buttons in the fluid supplier's GUI.
 *
 * <p>What travels is the change, not the new value: the buttons are "+1000", "-100" and so on, and
 * the client has no say in what the amount ends up being -- the server clamps it and answers with
 * {@link logisticspipes.network.to_client.pipe.FluidSupplierAmountMessage}. A change of zero is how the
 * GUI asks for the current amount when it opens.
 */
public record ChangeFluidSupplierAmountMessage(BlockPos pos, int change) implements CustomPacketPayload {

    public static final Type<ChangeFluidSupplierAmountMessage> TYPE =
            new Type<>(LPConstants.rl("change_fluid_supplier_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeFluidSupplierAmountMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChangeFluidSupplierAmountMessage::pos,
                    ByteBufCodecs.VAR_INT, ChangeFluidSupplierAmountMessage::change,
                    ChangeFluidSupplierAmountMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChangeFluidSupplierAmountMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeFluidSupplierMk2 pipe) {
            pipe.changeFluidAmount(message.change, context.player());
        }
    }
}
