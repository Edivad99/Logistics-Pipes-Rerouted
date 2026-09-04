package logisticspipes.network.to_client.pipe;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Where an item is inside a pipe, so the client can draw it moving.
 *
 * <p>Sent to everyone watching the chunk each time an item enters a pipe. The client does not
 * necessarily know the item yet -- it asks for the contents separately when it first sees an id.
 *
 * @param input  the side the item came in by, empty when it was injected rather than sent
 * @param output the side it is heading for, empty while the pipe has not decided
 */
public record TravellingItemPositionMessage(
        BlockPos pos,
        int travelId,
        Motion motion,
        Optional<Direction> input,
        Optional<Direction> output
) implements CustomPacketPayload {

    /** How the item is moving: where along the pipe it is, how fast, and which way it faces. */
    public record Motion(float position, float speed, float yaw) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Motion> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.FLOAT, Motion::position,
                        ByteBufCodecs.FLOAT, Motion::speed,
                        ByteBufCodecs.FLOAT, Motion::yaw,
                        Motion::new);
    }

    public static final Type<TravellingItemPositionMessage> TYPE =
            new Type<>(LPConstants.rl("travelling_item_position"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravellingItemPositionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TravellingItemPositionMessage::pos,
                    ByteBufCodecs.VAR_INT, TravellingItemPositionMessage::travelId,
                    Motion.STREAM_CODEC, TravellingItemPositionMessage::motion,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), TravellingItemPositionMessage::input,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), TravellingItemPositionMessage::output,
                    TravellingItemPositionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TravellingItemPositionMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || be.pipe == null || be.pipe.transport == null) {
            return;
        }
        be.pipe.transport.handleItemPositionPacket(message.travelId, message.input.orElse(null),
                message.output.orElse(null), message.motion.speed(), message.motion.position(),
                message.motion.yaw());
    }
}
