package logisticspipes.connection.special;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.connection.special.SpecialPipeConnection.ConnectionInformation;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;

public interface ISpecialPipedConnection {

	boolean isType(IPipeInformationProvider startPipe);

	List<ConnectionInformation> getConnections(IPipeInformationProvider startPipe, EnumSet<PipeRoutingConnectionType> connection,
			@Nullable Direction side);
}
