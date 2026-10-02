package logisticspipes.network.to_server.security;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

/**
 * The player authorized or deauthorized a security station.
 */
public record SetSecurityStationAuthorizedMessage(BlockPos pos, boolean authorized)
    implements CustomPacketPayload {

    public static final Type<SetSecurityStationAuthorizedMessage> TYPE =
        new Type<>(LPConstants.rl("set_security_station_authorized"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSecurityStationAuthorizedMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetSecurityStationAuthorizedMessage::pos,
            ByteBufCodecs.BOOL, SetSecurityStationAuthorizedMessage::authorized,
            SetSecurityStationAuthorizedMessage::new);

    public static void handle(SetSecurityStationAuthorizedMessage message, IPayloadContext context) {
        final LogisticsSecurityBlockEntity be = TargetLookup.securityStationAt(context.player(), message.pos);
        if (be == null) {
            return;
        }
        if (message.authorized) {
            be.authorizeStation();
        } else {
            be.deauthorizeStation();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
