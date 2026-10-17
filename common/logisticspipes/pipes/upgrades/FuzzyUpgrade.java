package logisticspipes.pipes.upgrades;

import java.util.List;

public class FuzzyUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "fuzzy";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of(UpgradeTarget.CRAFTING, UpgradeTarget.BASIC);
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of(UpgradeTarget.CRAFTING, UpgradeTarget.ITEM_SINK);
	}
}
