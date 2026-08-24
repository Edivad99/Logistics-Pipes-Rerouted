package logisticspipes.commands.commands;

// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class BypassCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "bypass", "bp" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return sender instanceof Player && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Allows to enable/disable the", "security station bypass token" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		if (!LogisticsSecurityTileEntity.byPassed.contains((Player) sender)) {
			LogisticsSecurityTileEntity.byPassed.add((Player) sender);
			sender.sendSystemMessage(Component.literal("Enabled"));
		} else {
			LogisticsSecurityTileEntity.byPassed.remove((Player) sender);
			sender.sendSystemMessage(Component.literal("Disabled"));
		}
	}
}
