package logisticspipes.network.to_client.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.StatisticsScreen;
import logisticspipes.network.TargetLookup;
import logisticspipes.util.TrackingTask;
import logisticspipes.world.level.block.entity.LogisticsStatisticsBlockEntity;

/**
 * What a statistics block has recorded so far, for the graph on its first tab.
 *
 * <p>Sent when the tracked items change and after every sample, so a screen left open keeps up
 * instead of showing what the network held when it was opened.
 */
public record TrackingTasksMessage(BlockPos pos, List<TrackingTask> tasks) implements CustomPacketPayload {

    public static final Type<TrackingTasksMessage> TYPE = new Type<>(LPConstants.rl("tracking_tasks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingTasksMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, TrackingTasksMessage::pos,
            TrackingTask.STREAM_CODEC.apply(ByteBufCodecs.list()), TrackingTasksMessage::tasks,
            TrackingTasksMessage::new);

    public static void handle(TrackingTasksMessage message, IPayloadContext context) {
        Client.handle(message, context);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final class Client {

        static void handle(TrackingTasksMessage message, IPayloadContext context) {
            final LogisticsStatisticsBlockEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsStatisticsBlockEntity.class);
            if (be == null) {
                return;
            }
            be.tasks = new ArrayList<>(message.tasks);
            if (Minecraft.getInstance().gui.screen() instanceof StatisticsScreen gui) {
                gui.handleTrackingTasks();
            }
        }
    }
}
