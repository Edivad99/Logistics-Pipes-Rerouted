package logisticspipes.pipes.upgrades;

import java.util.List;

public class PowerTransportationUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "power_transportation";
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
