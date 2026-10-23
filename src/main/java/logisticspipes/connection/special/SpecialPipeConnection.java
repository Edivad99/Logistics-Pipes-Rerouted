package logisticspipes.connection.special;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.Direction;

import lombok.Data;

import org.jspecify.annotations.Nullable;

import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;

public final class SpecialPipeConnection {

	private static @Nullable SpecialPipeConnection instance;

	private final List<ISpecialPipedConnection> handler;

	private SpecialPipeConnection(List<ISpecialPipedConnection> handler) {
		this.handler = handler;
	}

	/** Collects everything registered on {@code event}, which is closed from here on. */
	public static void load(RegisterSpecialConnectionsEvent event) {
		instance = new SpecialPipeConnection(event.registeredPipedConnections());
	}

	/**
	 * The pipe connection handlers collected at startup.
	 *
	 * @throws IllegalStateException before startup collected them
	 */
	public static SpecialPipeConnection get() {
		SpecialPipeConnection current = instance;
		if (current == null) {
			throw new IllegalStateException("SpecialPipeConnection is not loaded yet");
		}
		return current;
	}

	public List<ConnectionInformation> getConnectedPipes(IPipeInformationProvider startPipe, EnumSet<PipeRoutingConnectionType> connection,
			@Nullable Direction side) {
		for (ISpecialPipedConnection connectionHandler : handler) {
			if (connectionHandler.isType(startPipe)) {
				return connectionHandler.getConnections(startPipe, connection, side);
			}
		}
		return List.of();
	}

	/**
	 * One end of a special connection, as the handler that owns it describes it.
	 *
	 * <p>The two orientations are checked here rather than where they are used: the path finder
	 * stamps {@code exitOrientation} onto every route it found beyond this connection, and the
	 * router then reads it without asking. A handler that supplied null would crash several frames
	 * later, inside the routing table build, with nothing left to say who was at fault.
	 */
	@Data
	public static class ConnectionInformation {

		private IPipeInformationProvider connectedPipe;
		private EnumSet<PipeRoutingConnectionType> connectionFlags;
		private Direction insertOrientation;
		private Direction exitOrientation;
		private double distance;

		public ConnectionInformation(IPipeInformationProvider connectedPipe, EnumSet<PipeRoutingConnectionType> connectionFlags,
				Direction insertOrientation, Direction exitOrientation, double distance) {
			this.connectedPipe = Objects.requireNonNull(connectedPipe, "connectedPipe");
			this.connectionFlags = Objects.requireNonNull(connectionFlags, "connectionFlags");
			this.insertOrientation = Objects.requireNonNull(insertOrientation, "insertOrientation");
			this.exitOrientation = Objects.requireNonNull(exitOrientation, "exitOrientation");
			this.distance = distance;
		}
	}
}
