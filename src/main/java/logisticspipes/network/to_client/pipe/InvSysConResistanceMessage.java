package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The routing resistance an inventory system connector is set to, for its GUI to show.
 */
public record InvSysConResistanceMessage(BlockPos pos, int resistance) implements CustomPacketPayload {

    public static final Type<InvSysConResistanceMessage> TYPE =
            new Type<>(LPConstants.rl("inv_sys_con_resistance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSysConResistanceMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, InvSysConResistanceMessage::pos,
                    ByteBufCodecs.VAR_INT, InvSysConResistanceMessage::resistance,
                    InvSysConResistanceMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InvSysConResistanceMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeItemsInvSysConnector pipe) {
            pipe.resistance = message.resistance;
        }
    }
}
