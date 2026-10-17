package logisticspipes.pipes.upgrades;

import java.util.List;

public class OpaqueUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "opaque";
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
