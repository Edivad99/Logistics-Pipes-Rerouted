package logisticspipes.network.to_server;

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
 * The player changed an inventory system connector's routing resistance.
 *
 * <p>Applying it on the server also means telling the router, which is the whole point of the
 * setting: it is what the routing costs are worked out from.
 */
public record SetInvSysConResistanceMessage(BlockPos pos, int resistance) implements CustomPacketPayload {

    public static final Type<SetInvSysConResistanceMessage> TYPE =
            new Type<>(LPConstants.rl("set_inv_sys_con_resistance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetInvSysConResistanceMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetInvSysConResistanceMessage::pos,
                    ByteBufCodecs.VAR_INT, SetInvSysConResistanceMessage::resistance,
                    SetInvSysConResistanceMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetInvSysConResistanceMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeItemsInvSysConnector pipe) {
            pipe.resistance = message.resistance;
            pipe.getRouter().update(true, pipe);
        }
    }
}
