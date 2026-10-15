package logisticspipes.network.to_server.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.commands.Confirmations;
import logisticspipes.debug.DebugGuiController;
import logisticspipes.network.DebugTarget;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.debug.DebugController;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * What the player was pointing at, in answer to
 * {@link logisticspipes.network.to_client.debug.AskForDebugTargetMessage}.
 *
 * <p>Both debug tools ask the same question and get the same answer; {@link Purpose} says which of
 * them to hand it to. They used to be two packets whose ask halves were the same code twice, each
 * with its own copy of a three-valued mode enum.
 */
public record DebugTargetMessage(Purpose purpose, DebugTarget target) implements CustomPacketPayload {

    public static final Type<DebugTargetMessage> TYPE = new Type<>(LPConstants.rl("debug_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugTargetMessage> STREAM_CODEC =
        StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Purpose.class),
            DebugTargetMessage::purpose,
            DebugTarget.STREAM_CODEC, DebugTargetMessage::target,
            DebugTargetMessage::new);

    public static void handle(DebugTargetMessage message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (message.purpose) {
            case ROUTING_TABLE -> debugRoutingTable(player, message.target);
            case INSPECTOR -> inspect(player, message.target);
            case PIPE_LOG -> openPipeLog(player, message.target);
            case PIPE_DEBUG -> togglePipeDebug(player, message.target);
        }
    }

    private static void debugRoutingTable(ServerPlayer player, DebugTarget target) {
        if (!(target instanceof DebugTarget.Block(BlockPos pos))) {
            player.sendSystemMessage(Component.literal(
                target instanceof DebugTarget.Entity ? "Entities cannot be debugged this way"
                    : "No target found").withStyle(ChatFormatting.RED));
            return;
        }
        final LogisticsGenericPipeBlockEntity be =
            TargetLookup.blockEntityAt(player, pos, LogisticsGenericPipeBlockEntity.class);
        if (be == null || !(be.pipe instanceof CoreRoutedPipe pipe)
            || !(pipe.getRouter() instanceof ServerRouter router)) {
            player.sendSystemMessage(
                Component.literal("No routed pipe at " + pos).withStyle(ChatFormatting.RED));
            return;
        }
        Confirmations.ask(player, "Start a routing table debug update on the pipe at " + pos,
            asked -> {
                asked.sendSystemMessage(Component.literal("Starting routing table debug update.")
                    .withStyle(ChatFormatting.GREEN));
                DebugController.instance(asked).debug(router);
            });
    }

    private static void openPipeLog(ServerPlayer player, DebugTarget target) {
        if (!(target instanceof DebugTarget.Block(BlockPos pos))) {
            player.sendSystemMessage(Component.literal("Point at a pipe").withStyle(ChatFormatting.RED));
            return;
        }
        final LogisticsGenericPipeBlockEntity be =
            TargetLookup.blockEntityAt(player, pos, LogisticsGenericPipeBlockEntity.class);
        // The old packet cast the pipe to CoreRoutedPipe without asking first, so pointing at an
        // unrouted pipe was a ClassCastException in the handler.
        if (be == null || !(be.pipe instanceof CoreRoutedPipe pipe)) {
            player.sendSystemMessage(
                Component.literal("No routed pipe at " + pos).withStyle(ChatFormatting.RED));
            return;
        }
        pipe.debug.openForPlayer(player);
        player.sendSystemMessage(Component.literal("Debug log enabled."));
    }

    private static void togglePipeDebug(ServerPlayer player, DebugTarget target) {
        if (!(target instanceof DebugTarget.Block(BlockPos pos))) {
            player.sendSystemMessage(Component.literal("Point at a pipe").withStyle(ChatFormatting.RED));
            return;
        }
        final LogisticsGenericPipeBlockEntity be =
            TargetLookup.blockEntityAt(player, pos, LogisticsGenericPipeBlockEntity.class);
        if (be == null || !be.isInitialized()) {
            player.sendSystemMessage(
                Component.literal("No pipe at " + pos).withStyle(ChatFormatting.RED));
            return;
        }
        be.pipe.debug.debugThisPipe = !be.pipe.debug.debugThisPipe;
        player.sendSystemMessage(Component.literal(
            be.pipe.debug.debugThisPipe ? "Debug enabled on server" : "Debug disabled on server"));
    }

    private static void inspect(ServerPlayer player, DebugTarget target) {
        if (target instanceof DebugTarget.Block(BlockPos pos)) {
            final BlockEntity be = TargetLookup.blockEntityAt(player, pos, BlockEntity.class);
            if (be == null) {
                player.sendSystemMessage(
                    Component.literal("No block entity at " + pos).withStyle(ChatFormatting.RED));
                return;
            }
            Confirmations.ask(player, "Start debugging block entity " + be.getClass().getSimpleName(),
                asked -> DebugGuiController.instance().startWatchingOf(be, asked));
        } else if (target instanceof DebugTarget.Entity(int entityId)) {
            final Entity entity = player.level().getEntity(entityId);
            if (entity == null) {
                player.sendSystemMessage(Component.literal("No entity found").withStyle(ChatFormatting.RED));
                return;
            }
            Confirmations.ask(player, "Start debugging entity " + entity.getClass().getSimpleName(),
                asked -> DebugGuiController.instance().startWatchingOf(entity, asked));
        } else {
            player.sendSystemMessage(Component.literal("No target found").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Which debug tool asked.
     */
    public enum Purpose {
        /**
         * Step through a routing table update, one pipe at a time.
         */
        ROUTING_TABLE,
        /**
         * Watch a block entity's or an entity's fields live.
         */
        INSPECTOR,
        /**
         * Follow a pipe's own log in a window.
         */
        PIPE_LOG,
        /**
         * Turn the pipe log on or off for the server's copy of the pipe.
         */
        PIPE_DEBUG,
    }
}
