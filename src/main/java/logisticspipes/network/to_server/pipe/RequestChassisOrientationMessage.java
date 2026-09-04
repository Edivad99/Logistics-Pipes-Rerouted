package logisticspipes.network.to_server.pipe;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.ChassisOrientationMessage;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A chassis pipe has loaded on the client, which cannot tell from the block state which way it
 * points.
 */
public record RequestChassisOrientationMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestChassisOrientationMessage> TYPE =
            new Type<>(LPConstants.rl("request_chassis_orientation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestChassisOrientationMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestChassisOrientationMessage::pos,
                    RequestChassisOrientationMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestChassisOrientationMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof PipeLogisticsChassis chassis
                && context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ChassisOrientationMessage(
                    message.pos, Optional.ofNullable(chassis.getPointedOrientation())));
        }
    }
}
