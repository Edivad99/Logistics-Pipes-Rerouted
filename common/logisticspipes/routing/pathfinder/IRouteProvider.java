package logisticspipes.routing.pathfinder;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.core.Direction;

public interface IRouteProvider {

	@Data
	@AllArgsConstructor
	class RouteInfo {

		private IPipeInformationProvider pipe;
		private int length;
		private Direction exitOrientation;
	}

	List<RouteInfo> getConnectedPipes(Direction from);
}
