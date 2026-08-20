package logisticspipes.commands.commands;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.ticks.RoutingTableUpdateThread;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class RoutingThreadCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "routingthread", "rt" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display Routing thread status information" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		sender.displayClientMessage(Component.literal("RoutingTableUpdateThread: Queued: " + RoutingTableUpdateThread.size()), false);
		sender.displayClientMessage(Component.literal("RoutingTableUpdateThread: Average: " + RoutingTableUpdateThread.getAverage() + "ns"), false);
	}
}
