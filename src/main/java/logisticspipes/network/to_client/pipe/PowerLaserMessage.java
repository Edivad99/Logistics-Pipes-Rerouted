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
 * A power laser drawn along a pipe, or the removal of one.
 *
 * <p>Lasers exist only to be looked at, so the server tells the clients watching the chunk about
 * them and keeps nothing else in sync.
 *
 * @param direction  which side the beam runs along; empty for the ball, which has no direction
 * @param renderBall whether this is the ball at a junction rather than a beam between two
 * @param remove     whether the laser is going away rather than appearing
 */
public record PowerLaserMessage(
        BlockPos pos,
        Optional<Direction> direction,
        int color,
        float length,
        boolean reverse,
        boolean renderBall,
        boolean remove
) implements CustomPacketPayload {

    public static final Type<PowerLaserMessage> TYPE = new Type<>(LPConstants.rl("power_laser"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerLaserMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PowerLaserMessage::pos,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), PowerLaserMessage::direction,
                    ByteBufCodecs.INT, PowerLaserMessage::color,
                    ByteBufCodecs.FLOAT, PowerLaserMessage::length,
                    ByteBufCodecs.BOOL, PowerLaserMessage::reverse,
                    ByteBufCodecs.BOOL, PowerLaserMessage::renderBall,
                    ByteBufCodecs.BOOL, PowerLaserMessage::remove,
                    PowerLaserMessage::new);

    /** A laser appearing along {@code direction}. */
    public static PowerLaserMessage add(BlockPos pos, Direction direction, int color, float length,
            boolean reverse, boolean renderBall) {
        return new PowerLaserMessage(pos, Optional.ofNullable(direction), color, length, reverse, renderBall,
                false);
    }

    /** A laser going away. Only the colour, the side and the kind are needed to find it again. */
    public static PowerLaserMessage remove(BlockPos pos, Direction direction, int color, boolean renderBall) {
        return new PowerLaserMessage(pos, Optional.ofNullable(direction), color, 0, false, renderBall, true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PowerLaserMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null) {
            return;
        }
        final Direction direction = message.direction.orElse(null);
        if (message.remove) {
            be.removeLaser(direction, message.color, message.renderBall);
        } else {
            be.addLaser(direction, message.length, message.color, message.reverse, message.renderBall);
        }
    }
}
