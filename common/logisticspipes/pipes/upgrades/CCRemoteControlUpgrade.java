package logisticspipes.pipes.upgrades;

import java.util.List;

public class CCRemoteControlUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "cc_remote_control";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of(UpgradeTarget.ALL);
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of();
	}
}
