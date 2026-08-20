package logisticspipes.commands.commands.wrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import logisticspipes.asm.wrapper.AbstractWrapper;
import logisticspipes.asm.wrapper.LogisticsWrapperHandler;
import logisticspipes.asm.wrapper.WrapperState;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class ShowCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "show" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Re-enables the given proxy", "if it was disabled by an exception." };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		if (args.length != 1) {
			sender.displayClientMessage(Component.literal("Wrong amount of arguments"), false);
			return;
		}
		String name = args[0];
		List<AbstractWrapper> list = new ArrayList<>(LogisticsWrapperHandler.wrapperController);
		Iterator<AbstractWrapper> iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (item.getState() != WrapperState.Exception) {
				iter.remove();
			}
		}
		iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (!(item.getName() + item.getTypeName()).startsWith(name)) {
				iter.remove();
			}
		}
		if (list.size() > 1) {
			sender.displayClientMessage(Component.literal("Possible: "), false);
			for (AbstractWrapper can : list) {
				sender.displayClientMessage(Component.literal(can.getName() + can.getTypeName()), false);
			}
		} else if (list.isEmpty()) {
			sender.displayClientMessage(Component.literal("No match found"), false);
		} else {
			AbstractWrapper wrapper = list.get(0);
			Throwable t = wrapper.getReason();
			if (t == null) {
				sender.displayClientMessage(Component.literal("null"), false);
				return;
			} else {
				sender.displayClientMessage(Component.literal("-----------------------------------------------------"), false);
				sender.displayClientMessage(Component.literal(t.toString()), false);
				for (StackTraceElement s : t.getStackTrace()) {
					sender.displayClientMessage(Component.literal(" " + s.toString()), false);
				}
				t.printStackTrace();
			}
		}
	}
}
