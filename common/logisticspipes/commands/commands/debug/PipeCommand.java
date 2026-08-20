package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debug.PipeDebugLogAskForTarget;
import logisticspipes.network.packets.pipe.PipeDebugAskForTarget;
import logisticspipes.proxy.MainProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

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
			sender.displayClientMessage(Component.literal("Wrong amount of arguments"), false);
			return;
		}
		if (args[0].equalsIgnoreCase("help")) {
			sender.displayClientMessage(Component.literal("client, server, both or console"), false);
		} else if (args[0].equalsIgnoreCase("both")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(true), (Player) sender);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(false), (Player) sender);
			sender.displayClientMessage(Component.literal("Asking for Target."), false);
		} else if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("c")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugLogAskForTarget.class), (Player) sender);
			sender.displayClientMessage(Component.literal("Asking for Target."), false);
		} else {
			boolean isClient = args[0].equalsIgnoreCase("client");
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(!isClient), (Player) sender);
			sender.displayClientMessage(Component.literal("Asking for Target."), false);
		}
	}
}
