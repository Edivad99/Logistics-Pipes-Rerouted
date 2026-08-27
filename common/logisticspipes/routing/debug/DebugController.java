package logisticspipes.routing.debug;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.interfaces.IRoutingDebugAdapter;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.OpenChatGui;
import logisticspipes.network.packets.routingdebug.RoutingUpdateCanidatePipe;
import logisticspipes.network.packets.routingdebug.RoutingUpdateClearClient;
import logisticspipes.network.packets.routingdebug.RoutingUpdateDebugCanidateList;
import logisticspipes.network.packets.routingdebug.RoutingUpdateDebugClosedSet;
import logisticspipes.network.packets.routingdebug.RoutingUpdateDebugFilters;
import logisticspipes.network.packets.routingdebug.RoutingUpdateDoneDebug;
import logisticspipes.network.packets.routingdebug.RoutingUpdateInitDebug;
import logisticspipes.network.packets.routingdebug.RoutingUpdateSourcePipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.ticks.QueuedTasks;

public class DebugController implements IRoutingDebugAdapter {

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
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), sender);
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
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), sender);
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateDebugCanidateList.class).setExitRoutes(new ArrayList<>(candidatesCost)), sender);
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateClearClient.class), sender);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateSourcePipe.class).setExitRoute(lowestCostNode), sender);
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
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateDebugClosedSet.class).setPos(router.getLPPosition()).setSet(set), sender);
				}
			}
		}
		for (int i = 0; i < filterList.size(); i++) {
			EnumMap<PipeRoutingConnectionType, List<List<IFilter>>> filters = filterList.get(i);
			if (filters != null) {
				IRouter router = SimpleServiceLocator.routerManager.getRouter(i);
				if (router != null) {
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateDebugFilters.class).setPos(router.getLPPosition()).setFilters(filters), sender);
				}
			}
		}

		LinkedList<ExitRoute> exitRoutes = new LinkedList<>(candidatesCost);
		final ExitRoute nextNode = this.nextNode;
		if (flag && nextNode != null) {
			exitRoutes.addFirst(nextNode);
		}
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateDebugCanidateList.class).setExitRoutes(exitRoutes), sender);
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateCanidatePipe.class).setExitRoute(next), sender);
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateClearClient.class), sender);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateDoneDebug.class), sender);
		cachedRoutes.clear();
	}

	@Override
	public void init() {
		sendMsg("Initialising variables");
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateInitDebug.class), sender);
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
