package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debug.PipeDebugLogAskForTarget;
import logisticspipes.network.packets.pipe.PipeDebugAskForTarget;
import logisticspipes.proxy.MainProxy;

public class PipeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "pipe" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Set the pipe into debug mode" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		if (args.length != 1) {
			sender.sendSystemMessage(Component.literal("Wrong amount of arguments"));
			return;
		}
		if (args[0].equalsIgnoreCase("help")) {
			sender.sendSystemMessage(Component.literal("client, server, both or console"));
		} else if (args[0].equalsIgnoreCase("both")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(true), (Player) sender);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(false), (Player) sender);
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		} else if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("c")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugLogAskForTarget.class), (Player) sender);
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		} else {
			boolean isClient = args[0].equalsIgnoreCase("client");
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(!isClient), (Player) sender);
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		}
	}
}
