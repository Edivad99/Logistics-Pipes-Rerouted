package logisticspipes.network.to_server.security;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import logisticspipes.LPConstants;
import logisticspipes.network.to_client.security.PlayerListMessage;

/**
 * The security station's screen wants the names it can offer for autocompletion.
 */
public record RequestPlayerListMessage() implements CustomPacketPayload {

    public static final Type<RequestPlayerListMessage> TYPE =
            new Type<>(LPConstants.rl("request_player_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPlayerListMessage> STREAM_CODEC =
            StreamCodec.unit(new RequestPlayerListMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPlayerListMessage message, IPayloadContext context) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new PlayerListMessage(
                server.getPlayerList().getPlayers().stream()
                        .map(online -> online.getGameProfile().name())
                        .collect(java.util.stream.Collectors.toList())));
    }
}
