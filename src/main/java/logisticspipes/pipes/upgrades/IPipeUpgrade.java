package logisticspipes.pipes.upgrades;

import java.util.List;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public interface IPipeUpgrade {

    boolean needsUpdate();

    /**
     * The pipes this upgrade goes in, in the order the tooltip lists them.
     */
    List<UpgradeTarget> getAllowedPipes();

    /**
     * The modules this upgrade goes in, in the order the tooltip lists them.
     */
    List<UpgradeTarget> getAllowedModules();

    default boolean isAllowedForPipe(CoreRoutedPipe pipe) {
        return getAllowedPipes().stream().anyMatch(target -> target.accepts(pipe));
    }

    default boolean isAllowedForModule(LogisticsModule module) {
        return getAllowedModules().stream().anyMatch(target -> target.accepts(module));
    }
}
