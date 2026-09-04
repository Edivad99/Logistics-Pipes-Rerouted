package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.to_client.debug.AskForDebugTargetMessage;
import logisticspipes.network.to_client.debug.ToggleClientPipeDebugMessage;
import logisticspipes.network.to_server.debug.DebugTargetMessage.Purpose;

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
		} else if (sender instanceof ServerPlayer player) {
			if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("c")) {
				PacketDistributor.sendToPlayer(player, new AskForDebugTargetMessage(Purpose.PIPE_LOG));
			} else {
				if (!args[0].equalsIgnoreCase("client")) {
					PacketDistributor.sendToPlayer(player, new AskForDebugTargetMessage(Purpose.PIPE_DEBUG));
				}
				if (!args[0].equalsIgnoreCase("server")) {
					PacketDistributor.sendToPlayer(player, new ToggleClientPipeDebugMessage());
				}
			}
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		}
	}
}
