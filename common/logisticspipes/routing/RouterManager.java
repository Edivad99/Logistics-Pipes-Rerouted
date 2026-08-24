/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.ISecurityStationManager;
import logisticspipes.interfaces.routing.IChannelConnectionManager;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.SecurityStationAuthorizedList;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelConnection;

public class RouterManager implements IChannelConnectionManager, ISecurityStationManager {

	private final ArrayList<IRouter> routersClient = new ArrayList<>();
	private final ArrayList<ServerRouter> routersServer = new ArrayList<>();
	private final Map<UUID, Integer> uuidMap = new HashMap<>();

	private final WeakHashMap<LogisticsSecurityTileEntity, Void> security = new WeakHashMap<>();
	private List<String> authorized = new LinkedList<>();

	private final ArrayList<ChannelConnection> channelConnectedPipes = new ArrayList<>();

	@Nullable
	public IRouter getRouter(int id) {
		// MainProxy.isClient() checks Thread.currentThread() — fast, no world needed
		if (id <= 0 || MainProxy.isClient()) {
			return null;
		} else {
			return routersServer.get(id);
		}
	}

	@Nullable
	public ServerRouter getServerRouter(int id) {
		if (id <= 0) {
			return null;
		} else {
			return routersServer.get(id);
		}
	}

	public int getIDforUUID(UUID id) {
		if (id == null) {
			return -1;
		}
		Integer iId = uuidMap.get(id);
		if (iId == null) {
			return -1;
		}
		return iId;
	}

	public void removeRouter(int id) {
		// MainProxy.isClient() checks Thread.currentThread() — fast, no world needed.
		// During world unload the list may already have been cleared; tolerate out-of-range ids.
		if (!MainProxy.isClient() && id >= 0 && id < routersServer.size()) {
			routersServer.set(id, null);
		}
	}

	public IRouter getOrCreateRouter(UUID UUid, Level level, int xCoord, int yCoord, int zCoord) {
		IRouter r;
		int id = getIDforUUID(UUid);
		if (id > 0) {
			getRouter(id);
		}
		Identifier dimId = level.dimension().identifier();
		if (MainProxy.isClient(level)) {
			synchronized (routersClient) {
				for (IRouter r2 : routersClient) {
					if (r2.isAt(dimId, xCoord, yCoord, zCoord)) {
						return r2;
					}
				}
				r = new ClientRouter(UUid, dimId, xCoord, yCoord, zCoord);
				routersClient.add(r);
			}
		} else {
			synchronized (routersServer) {
				for (IRouter r2 : routersServer) {
					if (r2 != null && r2.isAt(dimId, xCoord, yCoord, zCoord)) {
						return r2;
					}
				}
				final ServerRouter serverRouter = new ServerRouter(UUid, dimId, xCoord, yCoord, zCoord);

				int rId = serverRouter.getSimpleID();
				if (routersServer.size() <= rId) {
					routersServer.ensureCapacity(rId + 1);
					while (routersServer.size() <= rId) {
						routersServer.add(null);
					}
				}
				routersServer.set(rId, serverRouter);
				uuidMap.put(serverRouter.getId(), serverRouter.getSimpleID());
				r = serverRouter;
			}
		}
		return r;
	}

	/**
	 * This assumes you know what you are doing. expect exceptions to be thrown
	 * if you pass the wrong side.
	 *
	 * @param id
	 * @param side
	 *            false for server, true for client.
	 * @return is this a router for the side.
	 */
	public boolean isRouterUnsafe(int id, boolean side) {
		if (side) {
			return true;
		} else {
			return routersServer.get(id) != null;
		}
	}

	public List<IRouter> getRouters() {
		if (MainProxy.isClient()) {
			return Collections.unmodifiableList(routersClient);
		} else {
			return Collections.unmodifiableList(routersServer);
		}
	}

	@Override
	public boolean hasChannelConnection(IRouter router) {
		return channelConnectedPipes.stream()
				.filter(con -> con.routers.size() > 1)
				.anyMatch(con -> con.routers.contains(router.getSimpleID()));
	}

	@Override
	public boolean addChannelConnection(UUID ident, IRouter router) {
		if (MainProxy.isClient()) {
			return false;
		}
		int routerSimpleID = router.getSimpleID();
		channelConnectedPipes.forEach(con -> con.routers.remove(routerSimpleID));
		Optional<ChannelConnection> channel = channelConnectedPipes.stream().filter(con -> con.identifier.equals(ident)).findFirst();
		if (channel.isPresent()) {
			channel.get().routers.add(routerSimpleID);
		} else {
			ChannelConnection newChannel = new ChannelConnection();
			channelConnectedPipes.add(newChannel);
			newChannel.identifier = ident;
			newChannel.routers.add(routerSimpleID);
		}
		return true;
	}

	@Override
	public List<CoreRoutedPipe> getConnectedPipes(IRouter router) {
		Optional<ChannelConnection> channel = channelConnectedPipes.stream()
				.filter(con -> con.routers.contains(router.getSimpleID()))
				.findFirst();
		return channel.
				map(channelConnection ->
						channelConnection.routers.stream()
								.filter(r -> r != router.getSimpleID())
								.map(this::getRouter).filter(Objects::nonNull)
								.map(IRouter::getPipe).filter(Objects::nonNull)
								.collect(Collectors.toList())
				)
				.orElse(Collections.emptyList());
	}

	@Override
	public void removeChannelConnection(IRouter router) {
		if (MainProxy.isClient()) {
			return;
		}
		Optional<ChannelConnection> channel = channelConnectedPipes.stream()
				.filter(con -> con.routers.contains(router.getSimpleID()))
				.findFirst();
		channel.ifPresent(chan -> chan.routers.remove(router.getSimpleID()));
		if (channel.filter(chan -> chan.routers.isEmpty()).isPresent()) {
			channelConnectedPipes.remove(channel.get());
		}
	}

	public void serverStopClean() {
		channelConnectedPipes.clear();
		routersServer.clear();
		uuidMap.clear();
		security.clear();
	}

	public void clearClientRouters() {
		synchronized (routersClient) {
			routersClient.clear();
		}
	}

	@Override
	public void add(LogisticsSecurityTileEntity tile) {
		security.put(tile, null);
		authorizeUUID(tile.getSecId());
	}

	@Override
	public LogisticsSecurityTileEntity getStation(UUID id) {
		if (id == null) {
			return null;
		}
		for (LogisticsSecurityTileEntity tile : security.keySet()) {
			if (id.equals(tile.getSecId())) {
				return tile;
			}
		}
		return null;
	}

	@Override
	public void remove(LogisticsSecurityTileEntity tile) {
		security.remove(tile);
		deauthorizeUUID(tile.getSecId());
	}

	public void dimensionUnloaded(Identifier dim) {
		synchronized (routersServer) {
			routersServer.stream().filter(r -> r != null && r.isInDim(dim)).forEach(r -> {
				r.clearPipeCache();
				r.clearInterests();
			});
		}
	}

	@Override
	public void deauthorizeUUID(UUID id) {
		authorized.remove(id.toString());
		sendClientAuthorizationList();
	}

	@Override
	public void authorizeUUID(UUID id) {
		if (!authorized.contains(id.toString())) {
			authorized.add(id.toString());
		}
		sendClientAuthorizationList();
	}

	@Override
	public boolean isAuthorized(@Nullable UUID id) {
		if (authorized.isEmpty() || id == null) {
			return false;
		}
		return authorized.contains(id.toString());
	}

	@Override
	public boolean isAuthorized(String id) {
		if (authorized.isEmpty() || id == null) {
			return false;
		}
		return authorized.contains(id);
	}

	@Override
	public void setClientAuthorizationList(List<String> list) {
		authorized = list;
	}

	@Override
	public void sendClientAuthorizationList() {
		MainProxy.sendToAllPlayers(PacketHandler.getPacket(SecurityStationAuthorizedList.class).setStringList(authorized));
	}

	@Override
	public void sendClientAuthorizationList(Player player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationAuthorizedList.class).setStringList(authorized), player);
	}

	public void printAllRouters() {
		routersServer.stream().filter(router -> router != null).forEach(router -> LogisticsPipes.LOG.info("{}", router));
	}
}
