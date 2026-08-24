package logisticspipes.commands.commands.debug;

// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import logisticspipes.commands.abstracts.ICommandHandler;

public class HandCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "hand" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the selected ItemStack" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		Player player = (Player) sender;
		ItemStack item = player.getInventory().getItem(player.getInventory().getSelectedSlot());
		if (!item.isEmpty()) {
			DebugGuiController.instance().startWatchingOf(item, player);
			sender.sendSystemMessage(Component.literal("Starting HandDebuging"));
		}
	}
}
