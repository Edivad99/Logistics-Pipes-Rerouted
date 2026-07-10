package logisticspipes.commands.commands;

import logisticspipes.LogisticsPipes;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.commands.chathelper.MorePageDisplay;
import logisticspipes.ticks.VersionChecker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// Player removed — use net.minecraft.commands.CommandSourceStack

public class ChangelogCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "changelog" };
	}

	@Override
	public boolean isCommandUsableBy(Player sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display the changelog between this version", "and the newest one" };
	}

	@Override
	public void executeCommand(Player sender, String[] args) {
		VersionChecker versionChecker = LogisticsPipes.versionChecker;
		String statusMessage = versionChecker.getVersionCheckerStatus();

		if (versionChecker.isVersionCheckDone() && versionChecker.getVersionInfo().isNewVersionAvailable()) {
			VersionChecker.VersionInfo versionInfo = versionChecker.getVersionInfo();

			MorePageDisplay display = new MorePageDisplay(new String[] { "(The newest version is #" + versionInfo.getNewestBuild() + ")", "< Changelog Page %/$ >" }, sender);
			if (versionInfo.getChangelog().isEmpty()) {
				display.append("No commits since your version.");
			} else {
				versionInfo.getChangelog().forEach(display::append);
			}
			display.display(sender);
		} else {
			sender.sendSystemMessage(Component.literal(statusMessage));
		}
	}
}
