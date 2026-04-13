package logisticspipes.commands.commands;

// Player removed — use net.minecraft.commands.CommandSourceStack
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.RequestUpdateNamesPacket;
import logisticspipes.proxy.MainProxy;

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
		sender.sendSystemMessage(Component.literal("Requesting Transfer"));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RequestUpdateNamesPacket.class), (Player) sender);
		MainProxy.proxy.sendNameUpdateRequest((Player) sender);
	}
}
