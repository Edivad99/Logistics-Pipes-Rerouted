package logisticspipes.commands;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

/**
 * {@code /logisticspipes retest} — reruns the in-game test suite.
 *
 * <p>Only registered when the game was started with {@code -Dlogisticspipes.test}; the suite lives
 * in the test source set, so it is reached by reflection.
 */
final class TestsCommand {

    private TestsCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("retest")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(TestsCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final Class<?> testClass;
        try {
            testClass = Class.forName("network.rs485.logisticspipes.integration.MinecraftTest");
        } catch (ReflectiveOperationException e) {
            source.sendFailure(Component.literal("Error loading minecraft test class " + e));
            return 0;
        }
        try {
            final Object instance = testClass.getDeclaredField("INSTANCE").get(null);
            final Method startTests = testClass.getDeclaredMethod("startTests", Consumer.class);
            final CompletableFuture<?> done = (CompletableFuture<?>) startTests.invoke(instance,
                (Consumer<Object>) msg -> source.sendSystemMessage(Component.literal(String.valueOf(msg))));
            done.whenComplete((ignored, throwable) -> {
                if (throwable == null) {
                    source.sendSystemMessage(Component.literal("SUCCESS").withStyle(ChatFormatting.GREEN));
                } else {
                    source.sendSystemMessage(
                        Component.literal("Tests failed with: " + throwable).withStyle(ChatFormatting.RED));
                }
            });
            return 1;
        } catch (ReflectiveOperationException | ClassCastException e) {
            source.sendFailure(Component.literal("Error accessing minecraft test instance " + e));
            return 0;
        }
    }
}
