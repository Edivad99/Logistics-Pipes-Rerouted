package logisticspipes.commands.commands;

import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.Job;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.utils.string.ChatColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class TestCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "retest" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Reruns all tests" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		final Class<?> testClass;
		try {
			testClass = Class.forName("network.rs485.logisticspipes.integration.MinecraftTest");
		} catch (ReflectiveOperationException e) {
			sender.displayClientMessage(Component.literal(ChatColor.RED + "Error loading minecraft test class " + e), false);
			return;
		}
		final Object minecraftTestInstance;
		try {
			minecraftTestInstance = testClass.getDeclaredField("INSTANCE").get(null);
			final Method startTestsMethod = testClass.getDeclaredMethod("startTests", Function1.class);
			final Job job = (Job) startTestsMethod.invoke(minecraftTestInstance, (Function1<Object, Unit>) msg -> {
				sender.displayClientMessage(Component.literal(String.valueOf(msg)), false);
				return Unit.INSTANCE;
			});
			job.invokeOnCompletion(throwable -> {
				if (throwable == null) {
					sender.displayClientMessage(Component.literal(ChatColor.GREEN + "SUCCESS"), false);
				} else {
					sender.displayClientMessage(Component.literal(ChatColor.RED + "Tests failed with: " + throwable), false);
				}
				return Unit.INSTANCE;
			});
		} catch (ReflectiveOperationException | ClassCastException e) {
			sender.displayClientMessage(Component.literal(ChatColor.RED + "Error accessing minecraft test instance " + e), false);
		}
	}
}
