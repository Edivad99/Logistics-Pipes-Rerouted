package logisticspipes.network.to_server.security;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

/**
 * The exclusion table was opened and wants the list it is about to show.
 */
public record RequestSecurityStationCCIdsMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestSecurityStationCCIdsMessage> TYPE =
        new Type<>(LPConstants.rl("request_security_station_cc_ids"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSecurityStationCCIdsMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, RequestSecurityStationCCIdsMessage::pos,
            RequestSecurityStationCCIdsMessage::new);

    public static void handle(RequestSecurityStationCCIdsMessage message, IPayloadContext context) {
        final LogisticsSecurityBlockEntity be = TargetLookup.securityStationAt(context.player(), message.pos);
        if (be != null) {
            be.requestList(context.player());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
