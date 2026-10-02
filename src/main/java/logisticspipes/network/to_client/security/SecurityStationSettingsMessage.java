package logisticspipes.network.to_client.security;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.SecurityStationScreen;
import logisticspipes.security.SecuritySettings;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity.SecurityPermissions;

/**
 * One player's security settings, for the station GUI that asked to edit them.
 *
 * <p>The id is the profile the name resolved to, absent if it did not; the popup sends it back with
 * its changes, so they land on the entry that was shown.
 */
public record SecurityStationSettingsMessage(String playerName, Optional<UUID> playerId,
    SecurityPermissions permissions)
    implements CustomPacketPayload {

    public static final Type<SecurityStationSettingsMessage> TYPE =
        new Type<>(LPConstants.rl("security_station_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityStationSettingsMessage> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SecurityStationSettingsMessage::playerName,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SecurityStationSettingsMessage::playerId,
            SecurityPermissions.STREAM_CODEC, SecurityStationSettingsMessage::permissions,
            SecurityStationSettingsMessage::new);

    public static void handle(SecurityStationSettingsMessage message, IPayloadContext context) {
        Client.handle(message, context);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final class Client {

        static void handle(SecurityStationSettingsMessage message, IPayloadContext context) {
            if (Minecraft.getInstance().screen instanceof SecurityStationScreen screen) {
                final SecuritySettings settings = new SecuritySettings(message.playerName);
                settings.id = message.playerId.orElse(null);
                message.permissions.applyTo(settings);
                screen.handlePlayerSecurityOpen(settings);
            }
        }
    }
}
