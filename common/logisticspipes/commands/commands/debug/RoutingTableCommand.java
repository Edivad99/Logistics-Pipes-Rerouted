package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.routingdebug.RoutingUpdateAskForTarget;
import logisticspipes.proxy.MainProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class RoutingTableCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "rt", "routing" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Starts debugging the Routing Table", "update of the pipe you are currently looking at." };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateAskForTarget.class), (Player) sender);
		sender.sendSystemMessage(Component.literal("Asking for Target."));
	}
}
