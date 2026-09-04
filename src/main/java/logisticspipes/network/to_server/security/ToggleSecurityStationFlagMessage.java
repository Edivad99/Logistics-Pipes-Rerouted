package logisticspipes.network.to_server.security;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity.SecurityFlag;
import logisticspipes.network.TargetLookup;

/**
 * The player ticked one of the security station's checkboxes.
 *
 * <p>Only which checkbox travels, not what it was set to: the station toggles, and answers with
 * {@link logisticspipes.network.to_client.security.SecurityStationFlagsMessage}. The old packet did send a
 * value, and the server threw it away -- so two players ticking at once could each be told the
 * opposite of what they clicked, which is correct, but only by accident.
 */
public record ToggleSecurityStationFlagMessage(BlockPos pos, SecurityFlag flag) implements CustomPacketPayload {

    public static final Type<ToggleSecurityStationFlagMessage> TYPE =
            new Type<>(LPConstants.rl("toggle_security_station_flag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSecurityStationFlagMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ToggleSecurityStationFlagMessage::pos,
                    NeoForgeStreamCodecs.enumCodec(SecurityFlag.class), ToggleSecurityStationFlagMessage::flag,
                    ToggleSecurityStationFlagMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSecurityStationFlagMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (be != null) {
            be.toggleFlag(message.flag);
        }
    }
}
