package logisticspipes.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.ticks.RoutingTableUpdateThread;
import logisticspipes.utils.FuzzyFlag;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

/**
 * {@code /logisticspipes}, and {@code /lp} for short.
 *
 * <p>Every subcommand is a node of the Brigadier tree, so the game handles completion, argument
 * parsing and permissions. Anything privileged asks for the gamemaster level the vanilla cheat
 * commands use, rather than checking the server's op list by name the way this used to.
 */
public final class LogisticsPipesCommand {

    private static @Nullable CommandDispatcher<CommandSourceStack> dispatcher = null;

    private LogisticsPipesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LogisticsPipesCommand.dispatcher = dispatcher;

        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(LPConstants.ID)
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(LogisticsPipesCommand::usage)
            .then(routingThread())
            .then(identify(buildContext))
            .then(dump())
            .then(bypass())
            .then(DebugCommand.build());
        if (LogisticsPipes.isTesting()) {
            root.then(TestsCommand.build());
        }

        final LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        // The short form forwards to the same tree, so it never drifts out of step with it.
        dispatcher.register(Commands.literal("lp")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(LogisticsPipesCommand::usage)
            .redirect(node));
    }

    /**
     * Lists what the player is allowed to run, straight out of the command tree.
     */
    private static int usage(CommandContext<CommandSourceStack> ctx) {
        final CommandDispatcher<CommandSourceStack> dispatcher = LogisticsPipesCommand.dispatcher;
        final CommandNode<CommandSourceStack> node =
            dispatcher == null ? null : dispatcher.getRoot().getChild(LPConstants.ID);
        if (node == null) {
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(LPConstants.NAME).withStyle(ChatFormatting.AQUA), false);
        final Map<CommandNode<CommandSourceStack>, String> usages =
            dispatcher.getSmartUsage(node, ctx.getSource());
        usages.values().forEach(usage -> ctx.getSource().sendSuccess(
            () -> Component.literal("/" + LPConstants.ID + " " + usage).withStyle(ChatFormatting.GRAY), false));
        return usages.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> routingThread() {
        return Commands.literal("routingthread")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Routing table updates queued: " + RoutingTableUpdateThread.size()), false);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Average update time: " + RoutingTableUpdateThread.getAverage() + "ns"), false);
                return 1;
            });
    }

    /**
     * Describes an item as LP identifies it.
     *
     * <p>This replaces a command that only echoed the item's display name, which every tooltip
     * already shows. What is worth asking the server is how it tells two look-alike stacks apart:
     * everything LP routes, filters and requests is keyed on an {@link ItemIdentifier}, whose
     * component patch is canonicalised on the way in, so what you typed is not always what it
     * compares on.
     *
     * <p>The old command took a numeric item id and a damage value, both of which stopped being how
     * items are addressed years ago. The item argument completes registry names and accepts the
     * component syntax used everywhere else, so {@code minecraft:iron_sword[damage=5]} works.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> identify(CommandBuildContext buildContext) {
        return Commands.literal("identify")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(LogisticsPipesCommand::describeHeldItem)
            .then(Commands.argument("item", ItemArgument.item(buildContext))
                .executes(ctx -> {
                    final ItemInput input = ItemArgument.getItem(ctx, "item");
                    return describeItem(ctx.getSource(),
                        ItemIdentifier.get(input.item().value(), input.components()));
                }));
    }

    /**
     * Describes what the player is holding.
     *
     * <p>Typing out the components of an item you already have is both tedious and beside the
     * point: the components a stack really carries are exactly what you cannot guess.
     */
    private static int describeHeldItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ItemStack held = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(
                "Hold an item, or name one: /" + LPConstants.ID + " identify <item>"));
            return 0;
        }
        return describeItem(ctx.getSource(), ItemIdentifier.get(held));
    }

    private static int describeItem(CommandSourceStack source, ItemIdentifier item) {
        source.sendSuccess(() -> Component.literal(item.getFriendlyName()).withStyle(ChatFormatting.AQUA), false);
        detail(source, "registry", String.valueOf(BuiltInRegistries.ITEM.getKey(item.item)));
        detail(source, "mod", item.getModName());

        final String components = item.describeComponents();
        detail(source, "components", components.isEmpty() ? "none" : components);
        if (item.isDamageable()) {
            detail(source, "damage", String.valueOf(item.getDamageValue()));
        }

        // Named after the flags in the filter GUI rather than after the projections behind them:
        // what the reader wants to know is whether ticking that box would change anything here.
        fuzzyFlag(source, FuzzyFlag.IGNORE_DAMAGE, item, item.getUndamaged());
        fuzzyFlag(source, FuzzyFlag.IGNORE_NBT, item, item.getIgnoringNBT());
        return 1;
    }

    private static void detail(CommandSourceStack source, String label, String value) {
        source.sendSuccess(() -> Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE)), false);
    }

    /**
     * Says whether a fuzzy filter flag would actually widen a match for this item.
     *
     * <p>The flag drops part of the identity; if what is left is the identity we started from,
     * there was nothing for it to drop and ticking the box here changes nothing.
     */
    private static void fuzzyFlag(CommandSourceStack source, FuzzyFlag flag, ItemIdentifier item,
        ItemIdentifier widened) {
        final List<String> dropped = droppedComponents(item, widened);
        if (dropped.isEmpty()) {
            detail(source, flag.name(), "no effect on this item");
        } else {
            detail(source, flag.name(), "also matches any " + String.join(", ", dropped));
        }
    }

    /**
     * The components the flag stops comparing on, named as the registry names them.
     *
     * <p>Both projections only ever forget components, so an empty result means the flag found
     * nothing here to drop. Naming what it drops answers the question someone actually has in front
     * of the filter GUI, which naming what survives does not.
     */
    private static List<String> droppedComponents(ItemIdentifier item, ItemIdentifier widened) {
        final Set<DataComponentType<?>> kept = widened.components.entrySet().stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        return item.components.entrySet().stream()
            .map(Map.Entry::getKey)
            .filter(type -> !kept.contains(type))
            .map(type -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)))
            .sorted()
            .toList();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dump() {
        return Commands.literal("dump")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(LogisticsPipesCommand::dumpThreads);
    }

    /**
     * Writes every thread's stack trace to its own file, for when a routing update wedges.
     *
     * <p>This used to go to the game log as one entry. Only its first line carried a timestamp and
     * a level, so the several hundred that followed sat in the log as bare {@code at ...} lines,
     * indistinguishable from a crash and duplicated into debug.log. A dump is a document, not a log
     * message; it goes next to the crash reports, where it can be opened and sent to someone.
     */
    private static int dumpThreads(CommandContext<CommandSourceStack> ctx) {
        final StringBuilder out = new StringBuilder();
        Thread.getAllStackTraces().forEach((thread, trace) -> {
            out.append('"').append(thread.getName()).append("\" ").append(thread.getState()).append('\n');
            for (StackTraceElement element : trace) {
                out.append("\tat ").append(element).append('\n');
            }
            out.append('\n');
        });

        final Path file = ctx.getSource().getServer().getServerDirectory()
            .resolve("logs")
            .resolve(LPConstants.ID + "-threads-" + Util.getFilenameFormattedDateTime() + ".txt");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out);
        } catch (IOException e) {
            LogisticsPipes.LOG.error("Could not write the thread dump", e);
            ctx.getSource().sendFailure(Component.literal("Could not write the thread dump: " + e));
            return 0;
        }

        // The server directory is relative in a development run, and a "./logs/..." on the
        // clipboard is no use anywhere else.
        final Path absolute = file.toAbsolutePath().normalize();
        LogisticsPipes.LOG.info("Thread dump written to {}", absolute);
        ctx.getSource().sendSuccess(() -> Component.literal("Thread dump written to ")
            .append(ChatUi.copyable(file.getFileName().toString(), absolute.toString(),
                "Click to copy " + absolute)), true);
        return 1;
    }

    /**
     * Lets the player walk past their own security stations until they turn it back off.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> bypass() {
        return Commands.literal("bypass")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(LogisticsPipesCommand::toggleBypass);
    }

    private static int toggleBypass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerPlayer player = ctx.getSource().getPlayerOrException();
        final boolean enabled;
        if (LogisticsSecurityBlockEntity.byPassed.contains(player)) {
            LogisticsSecurityBlockEntity.byPassed.remove(player);
            enabled = false;
        } else {
            LogisticsSecurityBlockEntity.byPassed.add(player);
            enabled = true;
        }
        ctx.getSource()
            .sendSuccess(() -> Component.literal("Security station bypass " + (enabled ? "enabled" : "disabled"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }
}
