package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debuggui.DebugAskForTarget;
import logisticspipes.proxy.MainProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DebugAskForTarget.class), (Player) sender);
		sender.displayClientMessage(Component.literal("Asking for Target."), false);
	}
}
