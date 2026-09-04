package logisticspipes.network.to_client.block;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.StatisticsScreen;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What every crafting pipe on the network is currently working on.
 */
public record RunningCraftingTasksMessage(List<ItemIdentifierStack> tasks) implements CustomPacketPayload {

    public static final Type<RunningCraftingTasksMessage> TYPE =
            new Type<>(LPConstants.rl("running_crafting_tasks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunningCraftingTasksMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    RunningCraftingTasksMessage::tasks,
                    RunningCraftingTasksMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RunningCraftingTasksMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof StatisticsScreen gui) {
            gui.handleRunningCraftingTasks(message.tasks);
        }
    }
}
