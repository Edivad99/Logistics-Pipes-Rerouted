package logisticspipes.routing.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import logisticspipes.interfaces.IDebugHUDProvider;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.particle.Particles;
import logisticspipes.particle.PipeFXRenderHandler;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.PipeRoutingConnectionType;
import net.minecraft.core.BlockPos;

import logisticspipes.util.DoubleCoordinates;

public class ClientViewController implements IDebugHUDProvider {

	private static ClientViewController instance;

	private ClientViewController() {}

	private DoubleCoordinates mainPipe = null;
	private int tick = 0;
	private final List<DoubleCoordinates> canidates = new ArrayList<>();
	private DebugWindow debugWindow;

	private List<IHeadUpDisplayRendererProvider> listHUD = new ArrayList<>();
	private HashMap<DoubleCoordinates, DebugInformation> HUDPositions = new HashMap<>();

	public static class DebugInformation {

		public boolean isNew = false;
		public int newIndex = -1;
		public List<Integer> positions = new ArrayList<>();
		public List<RouteDebugInfo> routes = new ArrayList<>();
		public Set<PipeRoutingConnectionType> closedSet;
		public Map<PipeRoutingConnectionType, List<List<BlockPos>>> filters;
		public Set<PipeRoutingConnectionType> nextFlags;
	}

	public static ClientViewController instance() {
		if (ClientViewController.instance == null) {
			ClientViewController.instance = new ClientViewController();
		}
		return ClientViewController.instance;
	}

	private DebugInformation getDebugInformation(DoubleCoordinates pos) {
		DebugInformation info = HUDPositions.get(pos);
		if (info == null) {
			info = new DebugInformation();
			HUDPositions.put(pos, info);
		}
		return info;
	}

	public void tick() {
		if (tick++ % 5 != 0) {
			return;
		}
		if (mainPipe != null) {
			PipeFXRenderHandler.spawnGenericParticle(Particles.WHITE_SPARKLE, mainPipe.getXInt(), mainPipe.getYInt(), mainPipe.getZInt(), 1);
		}
		for (DoubleCoordinates pos : canidates) {
			PipeFXRenderHandler.spawnGenericParticle(Particles.ORANGE_SPARKLE, pos.getXInt(), pos.getYInt(), pos.getZInt(), 1);
		}
	}

	public void clear() {
		mainPipe = null;
		canidates.clear();
		listHUD.clear();
		HUDPositions.clear();
	}

	public void setSource(RouteDebugInfo route) {
		mainPipe = new DoubleCoordinates(route.destination().getX(), route.destination().getY(),
				route.destination().getZ());
		getDebugInformation(mainPipe).nextFlags = route.flags();
	}

	public void addCandidate(RouteDebugInfo route) {
		DoubleCoordinates pos = new DoubleCoordinates(route.destination().getX(), route.destination().getY(),
				route.destination().getZ());
		canidates.add(pos);
		getDebugInformation(pos).isNew = true;
		getDebugInformation(pos).newIndex = route.index();
	}

	public void init() {
		debugWindow = new DebugWindow("Debug Code", 500, 250);
		LogisticsHUDRenderer.instance().debugHUD = this;
	}

	public void done() {
		if (debugWindow != null) {
			debugWindow.setVisible(false);
			debugWindow = null;
		}
		LogisticsHUDRenderer.instance().debugHUD = null;
		listHUD.clear();
		HUDPositions.clear();
	}

	public void setClosedSet(BlockPos pos, Set<PipeRoutingConnectionType> closed) {
		getDebugInformation(new DoubleCoordinates(pos.getX(), pos.getY(), pos.getZ())).closedSet = closed;
	}

	public void setFilters(BlockPos pos, Map<PipeRoutingConnectionType, List<List<BlockPos>>> filters) {
		getDebugInformation(new DoubleCoordinates(pos.getX(), pos.getY(), pos.getZ())).filters = filters;
	}

	public void updateList(List<RouteDebugInfo> routes) {
		debugWindow.clear();
		int i = 0;
		for (RouteDebugInfo route : routes) {
			i++;
			Color color = route.newlyAddedCandidate() ? Color.BLUE : Color.BLACK;
			debugWindow.showInfo(route.destinationName(), color);
			debugWindow.showInfo("\n", color);
			for (int j = 0; j < 2; j++) {
				debugWindow.showInfo("\t", color);
			}
			debugWindow.showInfo(route.networkDescription(), color);
			debugWindow.showInfo("\n", color);
			DoubleCoordinates pos = new DoubleCoordinates(route.destination().getX(),
					route.destination().getY(), route.destination().getZ());
			getDebugInformation(pos).routes.add(route);
			getDebugInformation(pos).positions.add(i);
		}
		listHUD.addAll(HUDPositions.entrySet().stream()
				.map(entry -> new HUDRoutingTableDebugProvider(new HUDRoutingTableGeneralInfo(entry.getValue()), entry.getKey()))
				.collect(Collectors.toList()));
	}

	@Override
	public List<IHeadUpDisplayRendererProvider> getHUDs() {
		return listHUD;
	}
}
