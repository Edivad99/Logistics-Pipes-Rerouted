package logisticspipes.routing.debug;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.commands.ChatUi;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateListMessage;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClearMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClosedSetMessage;
import logisticspipes.network.to_client.debug.RoutingDebugDoneMessage;
import logisticspipes.network.to_client.debug.RoutingDebugFiltersMessage;
import logisticspipes.network.to_client.debug.RoutingDebugInitMessage;
import logisticspipes.network.to_client.debug.RoutingDebugSourceMessage;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.ticks.QueuedTasks;

/**
 * Steps through a routing table update one pipe at a time, showing each step on the client.
 *
 * <p>The update runs on its own thread and parks between steps until the player asks for the next
 * one with {@code /logisticspipes debug step}, offered as buttons in chat.
 */
public class DebugController implements IRoutingDebugAdapter {

	/** What the player asked the parked update thread to do next. */
	public enum Step {
		/** Run until the next pipe, then ask again. */
		ONE,
		/** Run to the end without asking again. */
		ALL,
		/** Give up on this update. Set by {@link #stop()}, never asked for directly. */
		STOP,
	}

	private static final Map<UUID, DebugController> INSTANCES = new ConcurrentHashMap<>();

	/**
	 * The player's controller, created on first use.
	 *
	 * <p>Keyed by uuid rather than by the player object, which is replaced on respawn and on every
	 * dimension change — the old map handed those players a second, unrelated controller.
	 */
	public static DebugController instance(Player player) {
		return INSTANCES.computeIfAbsent(player.getUUID(), DebugController::new);
	}

	/** The player's controller, or null if they never started an update. */
	public static @Nullable DebugController active(Player player) {
		return INSTANCES.get(player.getUUID());
	}

	private final UUID playerId;

	public List<WeakReference<ExitRoute>> cachedRoutes = new LinkedList<>();

	private final Object stepLock = new Object();
	/** Whether the update thread is parked in {@link #awaitStep}. */
	private boolean waiting = false;
	/** The last answer, or null while there is none. Kept after the wait so ALL stays in force. */
	private @Nullable Step step = null;

	private volatile @Nullable Thread updateThread = null;
	private @Nullable ExitRoute prevNode = null;
	private @Nullable ExitRoute nextNode = null;
	private boolean pipeHandled = false;
	private @Nullable PriorityQueue<ExitRoute> candidatesCost = null;
	private @Nullable ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet = null;
	private @Nullable ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList = null;

	private DebugController(UUID playerId) {
		this.playerId = playerId;
	}

	private @Nullable ServerPlayer player() {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? null : server.getPlayerList().getPlayer(playerId);
	}

	private void sendToPlayer(CustomPacketPayload payload) {
		final ServerPlayer player = player();
		if (player != null) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	private void sendMsg(Component message) {
		final ServerPlayer player = player();
		if (player != null) {
			player.sendSystemMessage(message);
		}
	}

	private void sendMsg(String message) {
		sendMsg(Component.literal(message));
	}

	/** Filters travel as the positions of the pipes holding them; the client has no filters. */
	private static Map<PipeRoutingConnectionType, List<List<BlockPos>>> filterPositions(
			EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters) {
		final Map<PipeRoutingConnectionType, List<List<BlockPos>>> positions =
				new EnumMap<>(PipeRoutingConnectionType.class);
		filters.forEach((type, chains) -> positions.put(type, chains.stream()
				.map(chain -> chain.stream().map(filter -> filter.getPos()).toList())
				.toList()));
		return positions;
	}

	public void debug(final ServerRouter serverRouter) {
		QueuedTasks.queueTask(() -> {
			// This used to be oldThread.stop(), which throws UnsupportedOperationException outright
			// since Java 20. The previous run is canceled cooperatively instead: awaitStep() turns
			// the interrupt into a CancellationException that unwinds CreateRouteTable.
			final Thread previous = updateThread;
			if (previous != null) {
				previous.interrupt();
			}
			synchronized (stepLock) {
				waiting = false;
				step = null;
			}
			final Thread thread = new Thread() {

				@Override
				public void run() {
					try {
						serverRouter.CreateRouteTable(0, DebugController.this);
					} catch (CancellationException ignored) {
						// superseded by a newer debug run, or stopped by the player
					} finally {
						// stop() killed the thread outright, so it never got here. Now that it
						// unwinds, only clear the field if a newer run has not claimed it.
						if (updateThread == this) {
							updateThread = null;
						}
					}
				}
			};
			updateThread = thread;
			thread.setDaemon(true);
			thread.setName("[%s] RoutingTable update debug Thread".formatted(LPConstants.NAME));
			thread.start();
			return null;
		});
	}

	/**
	 * Lets a parked update carry on.
	 *
	 * @return false if nothing was waiting for an answer
	 */
	public boolean resume(Step requested) {
		synchronized (stepLock) {
			if (!waiting) {
				return false;
			}
			waiting = false;
			step = requested;
			stepLock.notifyAll();
		}
		return true;
	}

	/**
	 * Abandons the update.
	 *
	 * <p>A parked run is told to stop and unwinds on its own; one that is between pipes — which is
	 * where a run that was told to finish without asking spends all of its time — is interrupted,
	 * and unwinds the next time it parks or blocks.
	 *
	 * @return false if there was no update to stop
	 */
	public boolean stop() {
		final boolean parked;
		synchronized (stepLock) {
			parked = waiting;
			if (parked) {
				waiting = false;
				step = Step.STOP;
				stepLock.notifyAll();
			}
		}
		if (!parked) {
			final Thread thread = updateThread;
			if (thread == null) {
				return false;
			}
			thread.interrupt();
		}
		clearClientView();
		return true;
	}

	/** Takes the debug view, and with it the HUD, back off the client. */
	private void clearClientView() {
		sendToPlayer(new RoutingDebugClearMessage());
		sendToPlayer(new RoutingDebugDoneMessage());
		cachedRoutes.clear();
	}

	/** Whether an update is parked, waiting to be told what to do next. */
	public boolean isWaiting() {
		synchronized (stepLock) {
			return waiting;
		}
	}

	/**
	 * Parks the update thread until the player picks one of the buttons.
	 *
	 * <p>An interrupt lands here for most of a cancelled run's life; unwinding as a
	 * {@link CancellationException} is what makes {@link Thread#interrupt()} an actual replacement
	 * for the {@code Thread.stop()} this used to rely on.
	 */
	private void awaitStep(final String reason) {
		synchronized (stepLock) {
			if (step == Step.ALL) {
				return;
			}
			step = null;
			waiting = true;
			QueuedTasks.queueTask(() -> {
				prompt(reason);
				return null;
			});
			while (step == null) {
				try {
					stepLock.wait();
				} catch (InterruptedException e) {
					waiting = false;
					Thread.currentThread().interrupt();
					throw new CancellationException("Routing debug cancelled for " + playerId);
				}
			}
			waiting = false;
			if (step == Step.STOP) {
				throw new CancellationException("Routing debug stopped by " + playerId);
			}
		}
	}

	private void prompt(String reason) {
		final String command = "/" + LPConstants.ID + " debug step ";
		sendMsg(Component.literal(reason + " ").withStyle(ChatFormatting.AQUA)
				.append(ComponentUtils.formatList(List.of(
						ChatUi.button("Next", ChatFormatting.GREEN, command + "one", "Continue to the next pipe"),
						ChatUi.button("Rest", ChatFormatting.YELLOW, command + "all", "Finish without asking again"),
						ChatUi.button("Stop", ChatFormatting.RED, command + "stop", "Abandon this update"),
						ChatUi.button("List", ChatFormatting.AQUA, "/" + LPConstants.ID + " debug show",
								"Open the candidate list")),
						CommonComponents.SPACE)));
	}

	@Override
	public void start(PriorityQueue<ExitRoute> candidatesCost, ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet, ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList) {
		this.candidatesCost = candidatesCost;
		this.closedSet = closedSet;
		this.filterList = filterList;
		sendToPlayer(new RoutingDebugCandidateListMessage(
				candidatesCost.stream().map(RouteDebugInfo::of).toList()));
		awaitStep("Start?");
	}

	@Override
	public void nextPipe(ExitRoute lowestCostNode) {
		nextNode = lowestCostNode;
		if (!pipeHandled) {
			handledPipe(true);
		}
		pipeHandled = false;
		prevNode = lowestCostNode;
		sendToPlayer(new RoutingDebugClearMessage());
		sendToPlayer(new RoutingDebugSourceMessage(RouteDebugInfo.of(lowestCostNode)));
	}

	@Override
	public void handledPipe() {
		handledPipe(false);
	}

	public void handledPipe(boolean flag) {
		// These are only populated by start(); CreateRouteTable always calls it first, but a
		// canceled run can unwind out of awaitStep() and leave a later callback with nothing to send.
		final ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet = this.closedSet;
		final ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList = this.filterList;
		final PriorityQueue<ExitRoute> candidatesCost = this.candidatesCost;
		if (closedSet == null || filterList == null || candidatesCost == null) {
			return;
		}
		for (int i = 0; i < closedSet.size(); i++) {
			EnumSet<PipeRoutingConnectionType> set = closedSet.get(i);
			if (set != null) {
				IRouter router = RouterManager.get().getRouter(i);
				if (router != null) {
					sendToPlayer(new RoutingDebugClosedSetMessage(router.getPos(), set));
				}
			}
		}
		for (int i = 0; i < filterList.size(); i++) {
			EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters = filterList.get(i);
			if (filters != null) {
				IRouter router = RouterManager.get().getRouter(i);
				if (router != null) {
					sendToPlayer(new RoutingDebugFiltersMessage(router.getPos(),
						filterPositions(filters)));
				}
			}
		}

		LinkedList<ExitRoute> exitRoutes = new LinkedList<>(candidatesCost);
		final ExitRoute nextNode = this.nextNode;
		if (flag && nextNode != null) {
			exitRoutes.addFirst(nextNode);
		}
		sendToPlayer(new RoutingDebugCandidateListMessage(
				exitRoutes.stream().map(RouteDebugInfo::of).toList()));
		if (prevNode == null || prevNode.debug.isTraced) {
			//Display Information On Client Side

			awaitStep("Continue with next pipe?");
		}
		pipeHandled = true;
	}

	@Override
	public void newCandidate(ExitRoute next) {
		next.debug.index = cachedRoutes.size();
		cachedRoutes.add(new WeakReference<>(next));
		sendToPlayer(new RoutingDebugCandidateMessage(RouteDebugInfo.of(next)));
	}

	@Override
	public void stepOneDone() {
		sendMsg("Step One Finished");
	}

	@Override
	public void stepTwoDone() {
		sendMsg("Step Two Finished");
	}

	@Override
	public void done() {
		sendMsg("Update Done");
		clearClientView();
	}

	@Override
	public void init() {
		sendMsg("Initialising variables");
		sendToPlayer(new RoutingDebugInitMessage());
	}

	@Override
	public void newFlagsForPipe(EnumSet<PipeRoutingConnectionType> newFlags) {

	}

	@Override
	public void filterList(@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters) {

	}

	@Override
	public boolean independent() {
		return true;
	}

	@Override
	public boolean isDebug() {
		return true;
	}

	public void untrace(int integer) {
		// ref.get() has to be read once into a local: the referent can be collected between two
		// calls, so the old null-check-then-dereference pattern could still NPE.
		ExitRoute route = cachedRoutes.get(integer).get();
		if (route != null) {
			route.debug.isTraced = false;
			LogisticsPipes.LOG.debug("Did Untrack: {}", route.destination.getPos());
		}
	}
}
