package logisticspipes.pipes.upgrades;

import java.util.List;

public class SpeedUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "speed";
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
