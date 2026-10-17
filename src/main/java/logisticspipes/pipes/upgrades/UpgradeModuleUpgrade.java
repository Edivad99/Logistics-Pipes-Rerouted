package logisticspipes.pipes.upgrades;

import java.util.List;

public class UpgradeModuleUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "module_upgrade";
    }

    @Override
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public List<UpgradeTarget> getAllowedPipes() {
        return List.of(UpgradeTarget.CHASSIS);
    }

    @Override
    public List<UpgradeTarget> getAllowedModules() {
        return List.of();
    }
}
