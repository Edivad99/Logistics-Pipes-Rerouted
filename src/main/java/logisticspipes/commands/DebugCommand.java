package logisticspipes.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.to_client.debug.AskForDebugTargetMessage;
import logisticspipes.network.to_client.debug.OpenDebugScreenMessage;
import logisticspipes.network.to_client.debug.ToggleClientPipeDebugMessage;
import logisticspipes.network.to_server.debug.DebugTargetMessage.Purpose;
import logisticspipes.routing.debug.DebugController;

/**
 * {@code /logisticspipes debug} — the tools for looking inside a running game.
 *
 * <p>Everything here needs a player: the ones that debug what you are pointing at ask the client
 * what that is, and the answer comes back as a {@link logisticspipes.network.to_server.debug.DebugTargetMessage}.
 */
final class DebugCommand {

    private DebugCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("routing").executes(ask(Purpose.ROUTING_TABLE)))
            .then(Commands.literal("show").executes(open(OpenDebugScreenMessage.Screen.ROUTING)))
            // The three toggles sit under their own literal: they only flip a flag on the pipe,
            // which is a different job from log and show, and as flat siblings it was not
            // obvious that log is the one that actually starts collecting anything.
            .then(Commands.literal("pipe")
                .then(Commands.literal("log").executes(ask(Purpose.PIPE_LOG)))
                .then(Commands.literal("show").executes(open(OpenDebugScreenMessage.Screen.PIPE_LOG)))
                .then(Commands.literal("toggle")
                    .then(Commands.literal("client").executes(ctx -> pipeDebug(ctx, false, true)))
                    .then(Commands.literal("server").executes(ctx -> pipeDebug(ctx, true, false)))
                    .then(Commands.literal("both").executes(ctx -> pipeDebug(ctx, true, true)))))
            .then(Commands.literal("step")
                .then(Commands.literal("one").executes(step(DebugController.Step.ONE)))
                .then(Commands.literal("all").executes(step(DebugController.Step.ALL)))
                .then(Commands.literal("stop").executes(DebugCommand::stop)));
    }

    /**
     * Asks the client what the player is looking at.
     *
     * <p>The client answers with whatever the crosshair is on right now — which, with the chat open,
     * is whatever it was on when the chat was opened — so the pipe has to be lined up before the
     * command is typed. The answer arrives asynchronously, in a
     * {@link logisticspipes.network.to_server.debug.DebugTargetMessage}.
     */
    private static Command<CommandSourceStack> ask(Purpose purpose) {
        return ctx -> {
            send(ctx, new AskForDebugTargetMessage(purpose));
            ctx.getSource().sendSuccess(() -> Component.literal("Looking up what your crosshair is on."), false);
            return 1;
        };
    }

    /**
     * Reopens one of the client-side debug screens.
     */
    private static Command<CommandSourceStack> open(OpenDebugScreenMessage.Screen screen) {
        return ctx -> {
            send(ctx, new OpenDebugScreenMessage(screen));
            return 1;
        };
    }

    private static int pipeDebug(CommandContext<CommandSourceStack> ctx, boolean server, boolean client)
        throws CommandSyntaxException {
        if (server) {
            send(ctx, new AskForDebugTargetMessage(Purpose.PIPE_DEBUG));
        }
        if (client) {
            send(ctx, new ToggleClientPipeDebugMessage());
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Toggling the pipe your crosshair is on."), false);
        return 1;
    }

    /**
     * Answers the prompt a parked routing table update leaves in chat.
     */
    private static Command<CommandSourceStack> step(DebugController.Step requested) {
        return ctx -> {
            final DebugController controller = controllerOf(ctx);
            if (controller == null || !controller.resume(requested)) {
                ctx.getSource().sendFailure(Component.literal("No routing table update is waiting for you."));
                return 0;
            }
            return 1;
        };
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final DebugController controller = controllerOf(ctx);
        if (controller == null || !controller.stop()) {
            ctx.getSource().sendFailure(Component.literal("No routing table update to stop."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Stopped.").withStyle(ChatFormatting.RED), false);
        return 1;
    }

    private static @Nullable DebugController controllerOf(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        return DebugController.active(ctx.getSource().getPlayerOrException());
    }

    private static void send(CommandContext<CommandSourceStack> ctx, CustomPacketPayload payload)
        throws CommandSyntaxException {
        PacketDistributor.sendToPlayer(ctx.getSource().getPlayerOrException(), payload);
    }
}
