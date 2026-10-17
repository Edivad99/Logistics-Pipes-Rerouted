package logisticspipes.pipes.upgrades;

import java.util.List;

public class AdvancedSatelliteUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "satellite_advanced";
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
