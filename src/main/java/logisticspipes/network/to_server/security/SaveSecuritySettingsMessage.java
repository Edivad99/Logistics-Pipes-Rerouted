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
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity.SecurityPermissions;

/**
 * The security station's per-player switches, as edited in its popup.
 *
 * <p>Named permissions rather than a {@code CompoundTag}: this is the one direction a client can
 * write into the security store, and it should be able to say exactly these six things about
 * exactly one named player, not hand over arbitrary NBT.
 */
public record SaveSecuritySettingsMessage(BlockPos pos, String playerName, SecurityPermissions permissions)
    implements CustomPacketPayload {

    public static final Type<SaveSecuritySettingsMessage> TYPE =
        new Type<>(LPConstants.rl("save_security_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveSecuritySettingsMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, SaveSecuritySettingsMessage::pos,
            ByteBufCodecs.STRING_UTF8, SaveSecuritySettingsMessage::playerName,
            SecurityPermissions.STREAM_CODEC, SaveSecuritySettingsMessage::permissions,
            SaveSecuritySettingsMessage::new);

    public static void handle(SaveSecuritySettingsMessage message, IPayloadContext context) {
        if (message.playerName.isEmpty()) {
            return;
        }
        final LogisticsSecurityBlockEntity station = TargetLookup.securityStationAt(context.player(), message.pos);
        if (station != null) {
            station.saveSecuritySettings(message.playerName, message.permissions);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
