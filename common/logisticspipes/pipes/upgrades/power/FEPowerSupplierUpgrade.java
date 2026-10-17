package logisticspipes.pipes.upgrades.power;

import java.util.List;

import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.pipes.upgrades.UpgradeTarget;

public class FEPowerSupplierUpgrade implements IPipeUpgrade {

	public static String getName() {
		return "power_supplier_fe";
	}

	@Override
	public boolean needsUpdate() {
		return true;
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
