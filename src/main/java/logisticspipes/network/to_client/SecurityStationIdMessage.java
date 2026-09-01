package logisticspipes.network.to_client;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.TargetLookup;

/**
 * The identity a security station goes by, for the GUI to display.
 *
 * <p>Empty when the station has none yet. The old packet had no way to say that and wrote the two
 * halves of the UUID unconditionally, so a station without one threw on the sending side.
 */
public record SecurityStationIdMessage(BlockPos pos, Optional<UUID> id) implements CustomPacketPayload {

    public static final Type<SecurityStationIdMessage> TYPE =
            new Type<>(LPConstants.rl("security_station_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityStationIdMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SecurityStationIdMessage::pos,
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SecurityStationIdMessage::id,
                    SecurityStationIdMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityStationIdMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (be != null) {
            message.id.ifPresent(be::setClientUUID);
        }
    }
}
