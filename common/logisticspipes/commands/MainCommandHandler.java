package logisticspipes.commands;

import net.minecraft.world.entity.player.Player;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.commands.abstracts.SubCommandHandler;
import logisticspipes.commands.commands.BypassCommand;
import logisticspipes.commands.commands.ClearCommand;
import logisticspipes.commands.commands.DebugCommand;
import logisticspipes.commands.commands.DummyCommand;
import logisticspipes.commands.commands.DumpCommand;
import logisticspipes.commands.commands.NBTDebugCommand;
import logisticspipes.commands.commands.NameLookupCommand;
import logisticspipes.commands.commands.RoutingThreadCommand;
import logisticspipes.commands.commands.TestCommand;
import logisticspipes.commands.commands.WrapperCommand;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class MainCommandHandler extends SubCommandHandler {

	@Override
	public String[] getNames() {
		return new String[] {LPConstants.ID, "lp", "logipipes" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "The main LP command" };
	}

	@Override
	public void registerSubCommands() {
		registerSubCommand(new DummyCommand());
		registerSubCommand(new NBTDebugCommand());
		registerSubCommand(new RoutingThreadCommand());
		registerSubCommand(new NameLookupCommand());
		registerSubCommand(new DumpCommand());
		registerSubCommand(new BypassCommand());
		registerSubCommand(new DebugCommand());
		registerSubCommand(new WrapperCommand());
		if (LogisticsPipes.isTesting()) {
			registerSubCommand(new TestCommand());
		}
		registerSubCommand(new ClearCommand());
	}
}
