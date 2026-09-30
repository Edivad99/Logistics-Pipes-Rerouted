package logisticspipes.routing.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

import logisticspipes.client.renderer.hud.LogisticsHUDRenderer;
import logisticspipes.interfaces.IDebugHUDProvider;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.particle.Particles;
import logisticspipes.particle.PipeFXRenderHandler;
import logisticspipes.routing.PipeRoutingConnectionType;

public class ClientViewController implements IDebugHUDProvider {

	private static @Nullable ClientViewController instance;

	private ClientViewController() {}

	private @Nullable BlockPos mainPipe = null;
	private int tick = 0;
	private final List<BlockPos> canidates = new ArrayList<>();
	/** The candidate list as the debug screen shows it, rebuilt on every step. */
	private final List<Component> candidateLines = new ArrayList<>();

	private final List<IHeadUpDisplayRendererProvider> listHUD = new ArrayList<>();
	private final HashMap<BlockPos, DebugInformation> HUDPositions = new HashMap<>();

	public static class DebugInformation {

		public boolean isNew = false;
		public int newIndex = -1;
		public List<Integer> positions = new ArrayList<>();
		public List<RouteDebugInfo> routes = new ArrayList<>();
		public @Nullable Set<PipeRoutingConnectionType> closedSet;
		public @Nullable Map<PipeRoutingConnectionType, List<List<BlockPos>>> filters;
		public @Nullable Set<PipeRoutingConnectionType> nextFlags;
	}

	public static ClientViewController instance() {
		if (ClientViewController.instance == null) {
			ClientViewController.instance = new ClientViewController();
		}
		return ClientViewController.instance;
	}

	private DebugInformation getDebugInformation(BlockPos pos) {
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
			PipeFXRenderHandler.spawnGenericParticle(Particles.WHITE_SPARKLE, mainPipe.getX(), mainPipe.getY(), mainPipe.getZ(), 1);
		}
		for (BlockPos pos : canidates) {
			PipeFXRenderHandler.spawnGenericParticle(Particles.ORANGE_SPARKLE, pos.getX(), pos.getY(), pos.getZ(), 1);
		}
	}

	public void clear() {
		mainPipe = null;
		canidates.clear();
		listHUD.clear();
		HUDPositions.clear();
	}

	public void setSource(RouteDebugInfo route) {
		mainPipe = route.destination();
		getDebugInformation(mainPipe).nextFlags = route.flags();
	}

	public void addCandidate(RouteDebugInfo route) {
		BlockPos pos = route.destination();
		canidates.add(pos);
		getDebugInformation(pos).isNew = true;
		getDebugInformation(pos).newIndex = route.index();
	}

	public void init() {
		candidateLines.clear();
		LogisticsHUDRenderer.instance().debugHUD = this;
	}

	public List<Component> candidateLines() {
		return List.copyOf(candidateLines);
	}

	public void done() {
		candidateLines.clear();
		LogisticsHUDRenderer.instance().debugHUD = null;
		listHUD.clear();
		HUDPositions.clear();
	}

	public void setClosedSet(BlockPos pos, Set<PipeRoutingConnectionType> closed) {
		getDebugInformation(pos).closedSet = closed;
	}

	public void setFilters(BlockPos pos, Map<PipeRoutingConnectionType, List<List<BlockPos>>> filters) {
		getDebugInformation(pos).filters = filters;
	}

	public void updateList(List<RouteDebugInfo> routes) {
		candidateLines.clear();
		int i = 0;
		for (RouteDebugInfo route : routes) {
			i++;
			candidateLines.add(Component.literal(route.destinationName())
					.withStyle(route.newlyAddedCandidate() ? ChatFormatting.AQUA : ChatFormatting.WHITE));
			candidateLines.add(Component.literal("    " + route.networkDescription())
					.withStyle(ChatFormatting.GRAY));
			BlockPos pos = route.destination();
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
