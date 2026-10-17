package logisticspipes.pipes.upgrades;

import java.util.List;

public class PatternUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "pattern";
    }

    @Override
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public List<UpgradeTarget> getAllowedPipes() {
        return List.of(UpgradeTarget.SUPPLIER);
    }

    @Override
    public List<UpgradeTarget> getAllowedModules() {
        return List.of(UpgradeTarget.SUPPLIER);
    }
}
