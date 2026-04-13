package logisticspipes.commands.abstracts;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

public interface ICommandHandler {

	String[] getNames();

	boolean isCommandUsableBy(Player sender);

	String[] getDescription();

	void executeCommand(Player sender, String[] args);
}
