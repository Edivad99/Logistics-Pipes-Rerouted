package logisticspipes.network.to_server.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.network.DebugTarget;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.gui.OpenChatGuiMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.debug.DebugController;
import logisticspipes.commands.commands.debug.DebugGuiController;

/**
 * What the player was pointing at, in answer to
 * {@link logisticspipes.network.to_client.debug.AskForDebugTargetMessage}.
 *
 * <p>Both debug tools ask the same question and get the same answer; {@link Purpose} says which of
 * them to hand it to. They used to be two packets whose ask halves were the same code twice, each
 * with its own copy of a three-valued mode enum.
 */
public record DebugTargetMessage(Purpose purpose, DebugTarget target) implements CustomPacketPayload {

    /** Which debug tool asked. */
    public enum Purpose {
        /** Step through a routing table update, one pipe at a time. */
        ROUTING_TABLE,
        /** Watch a block entity's or an entity's fields live. */
        INSPECTOR,
    }

    public static final Type<DebugTargetMessage> TYPE = new Type<>(LPConstants.rl("debug_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugTargetMessage> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, Purpose>enumCodec(Purpose.class),
                    DebugTargetMessage::purpose,
                    DebugTarget.STREAM_CODEC, DebugTargetMessage::target,
                    DebugTargetMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DebugTargetMessage message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (message.purpose) {
            case ROUTING_TABLE -> debugRoutingTable(player, message.target);
            case INSPECTOR -> inspect(player, message.target);
        }
    }

    private static void debugRoutingTable(ServerPlayer player, DebugTarget target) {
        if (!(target instanceof DebugTarget.Block block)) {
            player.sendSystemMessage(Component.literal(
                    target instanceof DebugTarget.Entity ? "Entities cannot be debugged this way"
                            : "No target found").withStyle(ChatFormatting.RED));
            return;
        }
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(player, block.pos(), LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof CoreRoutedPipe pipe)
                || !(pipe.getRouter() instanceof ServerRouter router)) {
            player.sendSystemMessage(
                    Component.literal("No routed pipe at " + block.pos()).withStyle(ChatFormatting.RED));
            return;
        }
        confirm(player, "Start a routing table debug update on the pipe at " + block.pos(), () -> {
            player.sendSystemMessage(Component.literal("Starting routing table debug update.")
                    .withStyle(ChatFormatting.GREEN));
            DebugController.instance(player).debug(router);
        });
    }

    private static void inspect(ServerPlayer player, DebugTarget target) {
        if (target instanceof DebugTarget.Block block) {
            final BlockEntity be = TargetLookup.blockEntityAt(player, block.pos(), BlockEntity.class);
            if (be == null) {
                player.sendSystemMessage(
                        Component.literal("No block entity at " + block.pos()).withStyle(ChatFormatting.RED));
                return;
            }
            confirm(player, "Start debugging block entity " + be.getClass().getSimpleName(),
                    () -> DebugGuiController.instance().startWatchingOf(be, player));
        } else if (target instanceof DebugTarget.Entity wanted) {
            final Entity entity = player.level().getEntity(wanted.entityId());
            if (entity == null) {
                player.sendSystemMessage(Component.literal("No entity found").withStyle(ChatFormatting.RED));
                return;
            }
            confirm(player, "Start debugging entity " + entity.getClass().getSimpleName(),
                    () -> DebugGuiController.instance().startWatchingOf(entity, player));
        } else {
            player.sendSystemMessage(Component.literal("No target found").withStyle(ChatFormatting.RED));
        }
    }

    /** Asks in chat, and runs the action once the player types yes. */
    private static void confirm(ServerPlayer player, String question, Runnable action) {
        LPChatListener.addTask(() -> {
            action.run();
            PacketDistributor.sendToPlayer(player, new OpenChatGuiMessage());
            return true;
        }, player);
        player.sendSystemMessage(Component.literal(question + "? <yes/no>").withStyle(ChatFormatting.AQUA));
        PacketDistributor.sendToPlayer(player, new OpenChatGuiMessage());
    }
}
