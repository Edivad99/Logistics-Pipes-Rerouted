package logisticspipes.pipes.upgrades;

import java.util.List;

public class CraftingMonitoringUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "crafting_monitoring";
    }

    @Override
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public List<UpgradeTarget> getAllowedPipes() {
        return List.of(UpgradeTarget.REQUEST_BLOCK);
    }

    @Override
    public List<UpgradeTarget> getAllowedModules() {
        return List.of();
    }
}
