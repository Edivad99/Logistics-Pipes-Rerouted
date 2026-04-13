package logisticspipes.commands.commands;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.abstracts.ICommandHandler;

public class DummyCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "dummy" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "#This Command doesn't do anything" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {}
}
