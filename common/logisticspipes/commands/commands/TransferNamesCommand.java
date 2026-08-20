package logisticspipes.commands.commands;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.RequestUpdateNamesPacket;
import logisticspipes.proxy.MainProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class TransferNamesCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "transfernames", "tn" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Sends all item names form the client", "to the server to update the Language Database" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		sender.displayClientMessage(Component.literal("Requesting Transfer"), false);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RequestUpdateNamesPacket.class), sender);
		MainProxy.getProxy(false).sendNameUpdateRequest(sender);
	}
}
