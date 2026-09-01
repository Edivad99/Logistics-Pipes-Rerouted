package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.TargetLookup;

/**
 * The player added a computer to the station's exclusion list, or took one off it.
 *
 * <p>Add and remove are one message with a flag: they carry the same id, come from the same two
 * buttons of the same table, and the station answers both with the whole list.
 */
public record SetSecurityStationCCIdMessage(BlockPos pos, int computerId, boolean excluded)
        implements CustomPacketPayload {

    public static final Type<SetSecurityStationCCIdMessage> TYPE =
            new Type<>(LPConstants.rl("set_security_station_cc_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSecurityStationCCIdMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetSecurityStationCCIdMessage::pos,
                    ByteBufCodecs.VAR_INT, SetSecurityStationCCIdMessage::computerId,
                    ByteBufCodecs.BOOL, SetSecurityStationCCIdMessage::excluded,
                    SetSecurityStationCCIdMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetSecurityStationCCIdMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (be == null) {
            return;
        }
        if (message.excluded) {
            be.addCCToList(message.computerId);
        } else {
            be.removeCCFromList(message.computerId);
        }
        be.requestList(context.player());
    }
}
