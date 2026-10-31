package logisticspipes.routing.pathfinder;

import org.jspecify.annotations.Nullable;

public interface ISubMultiBlockPipeInformationProvider {

    @Nullable
    IPipeInformationProvider getMainTile();
}
