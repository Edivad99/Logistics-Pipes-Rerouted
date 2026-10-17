package logisticspipes.pipes.upgrades;

import java.util.List;

public class CombinedSneakyUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "sneaky_combination";
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
