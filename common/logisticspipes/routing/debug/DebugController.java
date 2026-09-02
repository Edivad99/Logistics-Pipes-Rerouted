package logisticspipes.routing.debug;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.interfaces.IRoutingDebugAdapter;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateListMessage;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClearMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClosedSetMessage;
import logisticspipes.network.to_client.debug.RoutingDebugDoneMessage;
import logisticspipes.network.to_client.debug.RoutingDebugFiltersMessage;
import logisticspipes.network.to_client.debug.RoutingDebugInitMessage;
import logisticspipes.network.to_client.debug.RoutingDebugSourceMessage;
import logisticspipes.network.to_client.gui.OpenChatGuiMessage;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.ticks.QueuedTasks;

public class DebugController implements IRoutingDebugAdapter {

	private void sendToPlayer(CustomPacketPayload payload) {
		if (sender instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, payload);
		}
	}

	/** Filters travel as the positions of the pipes holding them; the client has no filters. */
	private static Map<PipeRoutingConnectionType, List<List<BlockPos>>> filterPositions(
			EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters) {
		final Map<PipeRoutingConnectionType, List<List<BlockPos>>> positions =
				new EnumMap<>(PipeRoutingConnectionType.class);
		filters.forEach((type, chains) -> positions.put(type, chains.stream()
				.map(chain -> chain.stream().map(filter -> filter.getLPPosition().getBlockPos()).toList())
				.toList()));
		return positions;
	}

	private static final HashMap<Player, DebugController> instances = new HashMap<>();
	public List<WeakReference<ExitRoute>> cachedRoutes = new LinkedList<>();

	private final Player sender;

	private DebugController(Player sender) {
		this.sender = sender;
	}

	public static DebugController instance(Player sender) {
		if (DebugController.instances.get(sender) == null) {
			DebugController.instances.put(sender, new DebugController(sender));
		}
		return DebugController.instances.get(sender);
	}

	private enum DebugWaitState {
		LOOP,
		CONTINUE,
		NOWAIT
	}

	@Nullable
	private Thread oldThread = null;
	// Any value but NOWAIT preserves the previous behavior of an unset field: wait() only
	// short-circuits on NOWAIT, and debug() re-arms this to LOOP before anything reads it.
	private DebugWaitState state = DebugWaitState.LOOP;
	@Nullable
	private ExitRoute prevNode = null;
	@Nullable
	private ExitRoute nextNode = null;
	private boolean pipeHandled = false;
	@Nullable
	private PriorityQueue<ExitRoute> candidatesCost = null;
	@Nullable
	private ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet = null;
	@Nullable
	private ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList = null;

	public void debug(final ServerRouter serverRouter) {
		QueuedTasks.queueTask(() -> {
			state = DebugWaitState.LOOP;
			Thread tmp = new Thread() {

				@Override
				@SuppressWarnings("BusyWait") // same poll-based LPChatListener handshake as wait()
				public void run() {
					while (LPChatListener.existTaskFor(sender.getDisplayName().getString())) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
					if (sender instanceof ServerPlayer openChatFor) {
			PacketDistributor.sendToPlayer(openChatFor, new OpenChatGuiMessage());
		}
					// This used to be oldThread.stop(), which throws UnsupportedOperationException
					// outright since Java 20. The previous run is canceled cooperatively instead:
					// wait() turns the interrupt into a CancellationException that unwinds
					// CreateRouteTable.
					final Thread previous = oldThread;
					if (previous != null) {
						previous.interrupt();
					}
					oldThread = new RoutingTableDebugUpdateThread() {

						@Override
						public void run() {
							try {
								serverRouter.CreateRouteTable(0, DebugController.this);
							} catch (CancellationException ignored) {
								// superseded by a newer debug run
							} finally {
								// stop() killed the thread outright, so it never got here. Now that
								// it unwinds, only clear the field if a newer run has not claimed it.
								if (oldThread == this) {
									oldThread = null;
								}
							}
						}
					};
					oldThread.setDaemon(true);
                    oldThread.setName("[%s] RoutingTable update debug Thread".formatted(LPConstants.NAME));
					oldThread.start();
				}
			};
			tmp.setDaemon(true);
			tmp.setName("[%s] RoutingTable debug starter".formatted(LPConstants.NAME));
			tmp.start();
			return null;
		});
	}

	private void sendMsg(String message) {
		sender.sendSystemMessage(Component.literal(message));
	}

	// The handshake with LPChatListener is poll-based: the chat task flips the state from
	// another thread, so there is nothing to wait/notify on without reworking that listener.
	@SuppressWarnings("BusyWait")
	private synchronized void wait(final String reason) {
		if (state == DebugWaitState.NOWAIT) {
			return;
		}
		state = DebugWaitState.LOOP;
		QueuedTasks.queueTask(() -> {
			sender.sendSystemMessage(Component.literal(reason));
			LPChatListener.addTask(() -> {
				state = DebugWaitState.CONTINUE;
				if (sender instanceof ServerPlayer openChatFor) {
			PacketDistributor.sendToPlayer(openChatFor, new OpenChatGuiMessage());
		}
				return true;
			}, sender);
			return null;
		});
		boolean exist = false;
		while (state == DebugWaitState.LOOP) {
			if (LPChatListener.existTaskFor(sender.getDisplayName().getString())) {
				exist = true;
			} else {
				if (exist) {
					state = DebugWaitState.NOWAIT;
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// The routing debug thread parks here for most of its life, so this is where a
				// cancellation lands. Swallowing it would leave the superseded run spinning
				// forever; unwinding is what makes interrupt() an actual replacement for stop().
				Thread.currentThread().interrupt();
				throw new CancellationException("Routing debug cancelled for " + sender.getDisplayName().getString());
			}
		}
	}

	@Override
	public void start(PriorityQueue<ExitRoute> candidatesCost, ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet, ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList) {
		this.candidatesCost = candidatesCost;
		this.closedSet = closedSet;
		this.filterList = filterList;
		sendToPlayer(new RoutingDebugCandidateListMessage(
				candidatesCost.stream().map(RouteDebugInfo::of).toList()));
		wait("Start?");
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
		// canceled run can unwind out of wait() and leave a later callback with nothing to send.
		final ArrayList<@Nullable EnumSet<PipeRoutingConnectionType>> closedSet = this.closedSet;
		final ArrayList<@Nullable EnumMap<PipeRoutingConnectionType, List<List<IFilter>>>> filterList = this.filterList;
		final PriorityQueue<ExitRoute> candidatesCost = this.candidatesCost;
		if (closedSet == null || filterList == null || candidatesCost == null) {
			return;
		}
		for (int i = 0; i < closedSet.size(); i++) {
			EnumSet<PipeRoutingConnectionType> set = closedSet.get(i);
			if (set != null) {
				IRouter router = SimpleServiceLocator.routerManager.getRouter(i);
				if (router != null) {
					sendToPlayer(new RoutingDebugClosedSetMessage(router.getLPPosition().getBlockPos(), set));
				}
			}
		}
		for (int i = 0; i < filterList.size(); i++) {
			EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters = filterList.get(i);
			if (filters != null) {
				IRouter router = SimpleServiceLocator.routerManager.getRouter(i);
				if (router != null) {
					sendToPlayer(new RoutingDebugFiltersMessage(router.getLPPosition().getBlockPos(),
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

			wait("Continue with next pipe?");
		}
		pipeHandled = true;
	}

	@Override
	public void newCanidate(ExitRoute next) {
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
		sendToPlayer(new RoutingDebugClearMessage());
		sendToPlayer(new RoutingDebugDoneMessage());
		cachedRoutes.clear();
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
			LogisticsPipes.LOG.debug("Did Untrack: {}", route.destination.getLPPosition());
		}
	}
}
