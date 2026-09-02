package logisticspipes.network.to_client.module;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;

import network.rs485.logisticspipes.module.SneakyDirection;

/**
 * The sneaky direction a module settled on, for the players watching its HUD.
 *
 * <p>Sent on every change and again to each player as they start watching, which is what makes
 * dropping one harmless: the next subscribe re-sends it.
 */
public record SneakyDirectionMessage(ModuleTarget target, Optional<Direction> direction)
        implements CustomPacketPayload {

    public static final Type<SneakyDirectionMessage> TYPE =
            new Type<>(LPConstants.rl("sneaky_direction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SneakyDirectionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SneakyDirectionMessage::target,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC),
                    SneakyDirectionMessage::direction,
                    SneakyDirectionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SneakyDirectionMessage message, IPayloadContext context) {
        final SneakyDirection module = message.target.resolve(context.player(), SneakyDirection.class);
        if (module != null) {
            module.setSneakyDirection(message.direction.orElse(null));
        }
    }
}
