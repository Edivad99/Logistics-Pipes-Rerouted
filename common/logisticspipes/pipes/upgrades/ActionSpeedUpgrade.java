package logisticspipes.pipes.upgrades;

import java.util.List;

public class ActionSpeedUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "action_speed";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of();
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of(UpgradeTarget.EXTRACTOR, UpgradeTarget.ADVANCED_EXTRACTOR);
	}
}
