package logisticspipes.transport;

import java.util.UUID;

import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;

/**
 * The transport of the System Entrance Pipe: sends what enters it to the System Destination Pipe
 * tuned to the same frequency card.
 */
public class SystemEntranceTransport extends PipeTransportLogistics {

    public SystemEntranceTransport() {
        super(true);
    }

    @Override
    RoutingResult resolveDestination(LPTravelingItemServer data) {
        if ((data.getDestination() < 0 || data.getArrived())
            && getPipe() instanceof PipeItemsSystemEntranceLogistics pipe) {
            UUID frequency = pipe.getLocalFreqUUID();
            if (frequency != null && pipe.useEnergy(5)) {
                for (ExitRoute route : pipe.getRouter().getIRoutersByCost()) {
                    if (!route.containsFlag(PipeRoutingConnectionType.CAN_ROUTE_TO)) {
                        continue;
                    }
                    if (route.destination.getPipe() instanceof PipeItemsSystemDestinationLogistics destination
                        && frequency.equals(destination.getTargetUUID())) {
                        data.setDestination(destination.getRouter().getSimpleID());
                        data.setArrived(false);
                    }
                }
            }
        }
        return super.resolveDestination(data);
    }
}
