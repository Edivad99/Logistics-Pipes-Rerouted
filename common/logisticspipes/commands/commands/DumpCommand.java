package logisticspipes.commands.commands;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class DumpCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "dump" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Dumps the current Tread states", "into the server log" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		sender.sendSystemMessage(Component.literal("Dump Created"));
	}
}
