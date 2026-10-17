package logisticspipes.pipes.upgrades;

import java.util.List;

public class LogicControllerUpgrade implements IPipeUpgrade {

    public static String getName() {
        return "logic_controller";
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
