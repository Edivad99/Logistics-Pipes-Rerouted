package logisticspipes.pipes.upgrades;

import java.util.List;

public class FluidCraftingUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "fluid_crafting";
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public List<UpgradeTarget> getAllowedPipes() {
		return List.of(UpgradeTarget.CRAFTING);
	}

	@Override
	public List<UpgradeTarget> getAllowedModules() {
		return List.of(UpgradeTarget.CRAFTING);
	}
}
