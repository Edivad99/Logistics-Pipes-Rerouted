package logisticspipes.commands.commands;

import net.minecraft.world.entity.player.Player;

import logisticspipes.commands.abstracts.ICommandHandler;

// Player removed — use net.minecraft.commands.CommandSourceStack

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
