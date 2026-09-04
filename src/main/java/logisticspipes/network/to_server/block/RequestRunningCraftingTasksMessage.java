package logisticspipes.network.to_server.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.block.RunningCraftingTasksMessage;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The statistics block's crafting tab wants to know what the network is busy making.
 */
public record RequestRunningCraftingTasksMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestRunningCraftingTasksMessage> TYPE =
            new Type<>(LPConstants.rl("request_running_crafting_tasks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRunningCraftingTasksMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestRunningCraftingTasksMessage::pos,
                    RequestRunningCraftingTasksMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestRunningCraftingTasksMessage message, IPayloadContext context) {
        final LogisticsStatisticsTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsStatisticsTileEntity.class);
        if (be == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final CoreRoutedPipe pipe = be.getConnectedPipe();
        if (pipe == null) {
            return;
        }
        final List<ItemIdentifierStack> tasks = new ArrayList<>();
        for (ExitRoute route : pipe.getRouter().getIRoutersByCost()) {
            if (route != null && route.destination.getPipe() instanceof PipeItemsCraftingLogistics crafter) {
                tasks.addAll(crafter.getItemOrderManager().getContentList(player.level()));
            }
        }
        PacketDistributor.sendToPlayer(player, new RunningCraftingTasksMessage(tasks));
    }
}
