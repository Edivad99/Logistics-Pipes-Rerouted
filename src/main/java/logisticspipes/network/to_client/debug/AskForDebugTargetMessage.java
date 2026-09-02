package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.DebugTarget;
import logisticspipes.network.to_server.debug.DebugTargetMessage;
import logisticspipes.network.to_server.debug.DebugTargetMessage.Purpose;

/**
 * Asks the client what its crosshair is on.
 *
 * <p>Only the client knows: the server has no ray trace for a player's view. The answer comes back
 * as {@link DebugTargetMessage}, carrying the purpose so the server knows which of the two debug
 * tools asked.
 */
public record AskForDebugTargetMessage(Purpose purpose) implements CustomPacketPayload {

    public static final Type<AskForDebugTargetMessage> TYPE =
            new Type<>(LPConstants.rl("ask_for_debug_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AskForDebugTargetMessage> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, Purpose>enumCodec(Purpose.class),
                    AskForDebugTargetMessage::purpose,
                    AskForDebugTargetMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AskForDebugTargetMessage message, IPayloadContext context) {
        ClientPacketDistributor.sendToServer(new DebugTargetMessage(message.purpose, DebugTarget.lookedAt()));
    }
}
