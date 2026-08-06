package logisticspipes.commands.commands;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.commands.exception.MissingArgumentException;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class NameLookupCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "name" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Displays the serverside stored name for", "the <item id> and <meta data>" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		if (args.length < 2) {
			throw new MissingArgumentException();
		}
		String idString = args[0];
		String metaString = args[1];
		int id = Integer.valueOf(idString);
		int meta = Integer.valueOf(metaString);
		Item lookedUp = BuiltInRegistries.ITEM.byId(id);
		// The command still takes a meta/damage argument; in 1.21 that is the DAMAGE component.
		ItemIdentifier item = meta == 0
				? ItemIdentifier.get(lookedUp)
				: ItemIdentifier.get(lookedUp, DataComponentPatch.builder().set(DataComponents.DAMAGE, meta).build());
		sender.sendSystemMessage(Component.literal("Name: " + item.getFriendlyNameCC()));
	}
}
