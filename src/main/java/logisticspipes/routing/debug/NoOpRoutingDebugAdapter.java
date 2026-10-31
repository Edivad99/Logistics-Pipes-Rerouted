package logisticspipes.routing.debug;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.PriorityQueue;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;

public class NoOpRoutingDebugAdapter implements IRoutingDebugAdapter {

    @Override
    public void start(PriorityQueue<ExitRoute> candidatesCost,
        ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet,
        ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList) {
    }

    @Override
    public void nextPipe(ExitRoute lowestCostNode) {
    }

    @Override
    public void handledPipe() {
    }

    @Override
    public void newCandidate(ExitRoute next) {
    }

    @Override
    public void stepOneDone() {
    }

    @Override
    public void stepTwoDone() {
    }

    @Override
    public void done() {
    }

    @Override
    public void init() {
    }

    @Override
    public void newFlagsForPipe(EnumSet<PipeRoutingConnectionType> newFlags) {
    }

    @Override
    public void filterList(@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters) {
    }

    @Override
    public boolean independent() {
        return false;
    }

    @Override
    public boolean isDebug() {
        return false;
    }
}
