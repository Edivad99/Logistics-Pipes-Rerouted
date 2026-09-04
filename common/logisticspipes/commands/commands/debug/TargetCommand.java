package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.to_client.debug.AskForDebugTargetMessage;
import logisticspipes.network.to_server.debug.DebugTargetMessage.Purpose;

public class TargetCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "target", "look", "watch" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Starts debugging the BlockEntity", "or Entity you are currently looking at." };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		if (sender instanceof ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, new AskForDebugTargetMessage(Purpose.INSPECTOR));
		}
		sender.sendSystemMessage(Component.literal("Asking for Target."));
	}
}
