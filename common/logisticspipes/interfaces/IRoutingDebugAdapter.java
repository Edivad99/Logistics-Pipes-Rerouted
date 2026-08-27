package logisticspipes.interfaces;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.PriorityQueue;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;

public interface IRoutingDebugAdapter {

	// closedSet and filterList are sparse: ServerRouter pre-fills them with nulls and only
	// populates the slots Dijkstra actually reaches, hence the nullable element types.
	void start(PriorityQueue<ExitRoute> candidatesCost, ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet, ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList);

	void nextPipe(ExitRoute lowestCostNode);

	void handledPipe();

	void newCanidate(ExitRoute next);

	void stepOneDone();

	void stepTwoDone();

	void done();

	void init();

	void newFlagsForPipe(EnumSet<PipeRoutingConnectionType> newFlags);

	void filterList(@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters);

	boolean independent();

	boolean isDebug();

}
