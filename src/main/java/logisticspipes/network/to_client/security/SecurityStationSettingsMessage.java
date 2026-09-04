package logisticspipes.network.to_client.security;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.SecurityStationScreen;
import logisticspipes.blocks.LogisticsSecurityTileEntity.SecurityPermissions;
import logisticspipes.security.SecuritySettings;

/**
 * One player's security settings, for the station GUI that asked to edit them.
 */
public record SecurityStationSettingsMessage(String playerName, SecurityPermissions permissions)
        implements CustomPacketPayload {

    public static final Type<SecurityStationSettingsMessage> TYPE =
            new Type<>(LPConstants.rl("security_station_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityStationSettingsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SecurityStationSettingsMessage::playerName,
                    SecurityPermissions.STREAM_CODEC, SecurityStationSettingsMessage::permissions,
                    SecurityStationSettingsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityStationSettingsMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof SecurityStationScreen screen) {
            final SecuritySettings settings = new SecuritySettings(message.playerName);
            message.permissions.applyTo(settings);
            screen.handlePlayerSecurityOpen(settings);
        }
    }
}
