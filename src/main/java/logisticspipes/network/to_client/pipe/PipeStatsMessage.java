package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe.TrafficCounts;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A pipe's traffic counters, for the pipe controller's statistics tab.
 *
 * <p>Sent when a player opens the controller and again whenever the counters move.
 *
 * @param routingTableSize how many other pipes this one can reach; the client has no routing table
 *                         of its own to count
 */
public record PipeStatsMessage(BlockPos pos, TrafficCounts session, TrafficCounts lifetime, int routingTableSize)
        implements CustomPacketPayload {

    public static final Type<PipeStatsMessage> TYPE = new Type<>(LPConstants.rl("pipe_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeStatsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeStatsMessage::pos,
                    TrafficCounts.STREAM_CODEC, PipeStatsMessage::session,
                    TrafficCounts.STREAM_CODEC, PipeStatsMessage::lifetime,
                    ByteBufCodecs.VAR_INT, PipeStatsMessage::routingTableSize,
                    PipeStatsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeStatsMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof CoreRoutedPipe pipe) {
            pipe.applyStats(message.session, message.lifetime, message.routingTableSize);
        }
    }
}
