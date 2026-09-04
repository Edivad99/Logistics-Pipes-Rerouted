package logisticspipes.commands.commands.debug;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.bidirectional.DebugConnectionDataMessage;
import logisticspipes.network.to_client.debug.OpenDebugPanelMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.debug.api.IDataConnection;
import network.rs485.debug.api.IDebugGuiEntry;
import network.rs485.debug.api.IObjectIdentification;

public class DebugGuiController {

	transient private static DebugGuiController instance;

	private DebugGuiController() {}

	public static DebugGuiController instance() {
		if (DebugGuiController.instance == null) {
			DebugGuiController.instance = new DebugGuiController();
		}
		return DebugGuiController.instance;
	}

	public void execClient() {
		flushClientData();
		if (clientController != null) {
			clientController.exec();
		}
	}

	public void execServer() {
		serverDebugger.values().forEach(IDebugGuiEntry::exec);
	}

	private final HashMap<Player, IDebugGuiEntry> serverDebugger = new HashMap<>();
	private final List<IDataConnection> serverList = new LinkedList<>();

	private IDebugGuiEntry clientController = null;
	private final List<Future<IDataConnection>> clientList = new LinkedList<>();
	private final Map<Integer, List<byte[]>> pendingClientData = new HashMap<>();

	public void startWatchingOf(Object object, Player player) {
		if (object == null) {
			return;
		}
		IDebugGuiEntry entry = serverDebugger.get(player);
		if (entry == null) {
			try {
				entry = IDebugGuiEntry.create();
				serverDebugger.put(player, entry);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		if (entry == null) {
			System.out.println("DebugGui could not be loaded");
			return;
		}
		synchronized (serverList) {
			int identification = serverList.size();
			IDataConnection conIn = new DataConnectionServer(identification, player);
			serverList.add(entry.startServerDebugging(object, conIn, new ObjectIdentification()));
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer,
						new OpenDebugPanelMessage(object.getClass().getSimpleName(), identification));
			}
		}
	}

	public void createNewDebugGui(String name, int identification) {
		if (clientController == null) {
			try {
				clientController = IDebugGuiEntry.create();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
		}
		synchronized (clientList) {
			while (clientList.size() <= identification) clientList.add(null);
			clientList.set(identification, clientController.startClientDebugging(name, new DataConnectionClient(identification)));
		}
	}

	public void handleDataPacket(byte[] payload, int connectionId, Player player) {
		if (MainProxy.isServer(player.level())) {
			synchronized (serverList) {
				if (connectionId < 0 || connectionId >= serverList.size()) {
					return;
				}
				IDataConnection connection = serverList.get(connectionId);
				if (connection != null) {
					connection.passData(payload);
				}
			}
		} else {
			synchronized (clientList) {
				pendingClientData.computeIfAbsent(connectionId, id -> new ArrayList<>()).add(payload);
				flushClientData();
			}
		}
	}

	/**
	 * Hands the buffered data to every panel that has finished opening.
	 *
	 * <p>The panel is built on another thread, so its first messages can arrive before it exists.
	 * Payloads are never retried, so holding them here is the only way they survive the wait.
	 */
	private void flushClientData() {
		synchronized (clientList) {
			pendingClientData.entrySet().removeIf(pending -> {
				IDataConnection connection = clientConnection(pending.getKey());
				if (connection == null) {
					return false;
				}
				pending.getValue().forEach(connection::passData);
				return true;
			});
		}
	}

	private @Nullable IDataConnection clientConnection(int connectionId) {
		if (connectionId < 0 || connectionId >= clientList.size()) {
			return null;
		}
		Future<IDataConnection> future = clientList.get(connectionId);
		if (future == null || !future.isDone()) {
			return null;
		}
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}

	@AllArgsConstructor
	private class DataConnectionServer implements IDataConnection {

		private int identification;
		private Player player;

		@Override
		public void passData(byte[] packet) {
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new DebugConnectionDataMessage(identification, packet));
			}
		}

		@Override
		public void closeCon() {
			serverList.set(identification, null);
		}
	}

	@AllArgsConstructor
	private class DataConnectionClient implements IDataConnection {

		private int identification;

		@Override
		public void passData(byte[] packet) {
			ClientPacketDistributor.sendToServer(new DebugConnectionDataMessage(identification, packet));
		}

		@Override
		public void closeCon() {
			clientList.set(identification, null);
		}
	}

	private static class ObjectIdentification implements IObjectIdentification {

		@Override
		public boolean toStringObject(Object o) {
			return o.getClass() == Direction.class || o.getClass() == ItemIdentifier.class || o.getClass() == ItemIdentifierStack.class;
		}

		@Override
		public String handleObject(Object o) {
			if (o instanceof Level) {
				return ((Level) o).dimension().identifier().getPath(); // was: getWorldName
			}
			if (o != null && o.getClass().isArray() && Array.getLength(o) > 100) {
				return "(Too big)";
			}
			return null;
		}
	}
}
