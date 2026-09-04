package logisticspipes.network.to_server.channel;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.routing.channels.ChannelInformation.AccessRights;
import logisticspipes.utils.PlayerIdentifier;

/**
 * The player saved a channel in the channel manager.
 *
 * <p>Creating and editing are one message: the form is the same, and which of the two it is comes
 * down to whether the channel already has an identifier. They used to be two packets, the edit one
 * extending the create one so that its own fields were written after the parent's.
 *
 * @param channel  the channel being edited, empty when creating a new one
 * @param security the security station that guards the channel, empty when it is not secured
 */
public record SaveChannelMessage(Optional<UUID> channel, String name, AccessRights rights,
        Optional<UUID> security) implements CustomPacketPayload {

    public static final Type<SaveChannelMessage> TYPE = new Type<>(LPConstants.rl("save_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveChannelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SaveChannelMessage::channel,
                    ByteBufCodecs.STRING_UTF8, SaveChannelMessage::name,
                    NeoForgeStreamCodecs.enumCodec(AccessRights.class),
                    SaveChannelMessage::rights,
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SaveChannelMessage::security,
                    SaveChannelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveChannelMessage message, IPayloadContext context) {
        final IChannelManager manager =
                SimpleServiceLocator.channelManagerProvider.getChannelManager(context.player().level());
        final UUID security = message.security.orElse(null);
        if (message.channel.isEmpty()) {
            manager.createNewChannel(message.name, PlayerIdentifier.get(context.player()), message.rights, security);
            return;
        }
        final UUID id = message.channel.get();
        manager.getChannels().stream()
                .filter(channel -> channel.getChannelIdentifier().equals(id))
                .findFirst()
                .ifPresent(channel -> apply(manager, channel, message, security));
    }

    private static void apply(IChannelManager manager, ChannelInformation channel, SaveChannelMessage message,
            UUID security) {
        final UUID id = channel.getChannelIdentifier();
        if (!channel.getName().equals(message.name)) {
            manager.updateChannelName(id, message.name);
        }
        // Compared with Objects.equals: the old condition mixed equals with a reference comparison
        // on the same pair of nullable ids, so it was true whenever both were null.
        if (!channel.getRights().equals(message.rights)
                || !Objects.equals(channel.getResponsibleSecurityID(), security)) {
            manager.updateChannelRights(id, message.rights, security);
        }
    }
}
