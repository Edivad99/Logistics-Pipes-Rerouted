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
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Which side a chassis pipe points its modules at.
 *
 * <p>Empty when it points at nothing, which is a state the chassis can genuinely be in -- there may
 * be no inventory next to it at all.
 */
public record ChassisOrientationMessage(BlockPos pos, Optional<Direction> direction)
        implements CustomPacketPayload {

    public static final Type<ChassisOrientationMessage> TYPE =
            new Type<>(LPConstants.rl("chassis_orientation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChassisOrientationMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChassisOrientationMessage::pos,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), ChassisOrientationMessage::direction,
                    ChassisOrientationMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChassisOrientationMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeLogisticsChassis chassis) {
            chassis.setPointedOrientation(message.direction.orElse(null));
        }
    }
}
