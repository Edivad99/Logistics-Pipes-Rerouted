package logisticspipes.pipes.upgrades;

import java.util.List;

public class ItemStackExtractionUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "item_stack_extraction";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of(UpgradeTarget.CRAFTING, UpgradeTarget.PROVIDER);
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of(UpgradeTarget.CRAFTING, UpgradeTarget.PROVIDER, UpgradeTarget.EXTRACTOR, UpgradeTarget.ADVANCED_EXTRACTOR);
	}
}
