package logisticspipes.network.to_server;

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
 * The player picked a side in the sneaky configurator.
 *
 * <p>Empty means the default side, which is why it is an {@code Optional} and not a
 * {@code Direction}: "no sneaky direction" is a value the module holds, not a missing field.
 */
public record SetSneakyDirectionMessage(ModuleTarget target, Optional<Direction> direction)
        implements CustomPacketPayload {

    public static final Type<SetSneakyDirectionMessage> TYPE =
            new Type<>(LPConstants.rl("set_sneaky_direction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSneakyDirectionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SetSneakyDirectionMessage::target,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC),
                    SetSneakyDirectionMessage::direction,
                    SetSneakyDirectionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetSneakyDirectionMessage message, IPayloadContext context) {
        final SneakyDirection module = message.target.resolve(context.player(), SneakyDirection.class);
        if (module != null) {
            module.setSneakyDirection(message.direction.orElse(null));
        }
    }
}
