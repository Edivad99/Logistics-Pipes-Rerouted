package logisticspipes.network.to_client.security;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.proxy.SimpleServiceLocator;

/**
 * Who is authorized on the security stations, for the client's copy of the list.
 *
 * <p>Broadcast whenever it changes and sent again to each player as they log in, so a client that
 * misses one is made good at the next of either.
 */
public record SecurityAuthorizedListMessage(List<String> authorized) implements CustomPacketPayload {

    public static final Type<SecurityAuthorizedListMessage> TYPE =
            new Type<>(LPConstants.rl("security_authorized_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityAuthorizedListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SecurityAuthorizedListMessage::authorized,
                    SecurityAuthorizedListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityAuthorizedListMessage message, IPayloadContext context) {
        SimpleServiceLocator.securityStationManager.setClientAuthorizationList(message.authorized);
    }
}
