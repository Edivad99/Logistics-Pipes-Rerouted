package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.abstracts.ICommandHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "me", "self" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the CommandSender" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		DebugGuiController.instance().startWatchingOf(sender, (Player) sender);
		sender.displayClientMessage(Component.literal("Starting SelfDebugging"), false);
	}
}
