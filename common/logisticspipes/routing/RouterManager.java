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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.to_client.security.SecurityAuthorizedListMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.channels.ChannelConnection;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

/**
 * The routers, security stations and channel connections of the running server.
 *
 * <p>Replaced by an empty one when the server stops, so nothing of one world is left over in the
 * next one of a singleplayer session.
 */
public final class RouterManager {

	private static volatile RouterManager instance = new RouterManager();

	private final ArrayList<@Nullable ServerRouter> routersServer = new ArrayList<>();
	private final Map<UUID, Integer> uuidMap = new HashMap<>();

	private final WeakHashMap<LogisticsSecurityBlockEntity, @Nullable Void> security = new WeakHashMap<>();
	private final List<String> authorized = new LinkedList<>();

	private final ArrayList<ChannelConnection> channelConnectedPipes = new ArrayList<>();

	private RouterManager() {
	}

	public static RouterManager get() {
		return instance;
	}

	public static void reset() {
		instance = new RouterManager();
	}

	@Nullable
	public IRouter getRouter(int id) {
		if (id <= 0 || id >= routersServer.size()) {
			return null;
		}
		return routersServer.get(id);
	}

	@Nullable
	public ServerRouter getServerRouter(int id) {
		if (id <= 0) {
			return null;
		} else {
			return routersServer.get(id);
		}
	}

	public int getIDforUUID(@Nullable UUID id) {
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
		if (id >= 0 && id < routersServer.size()) {
			routersServer.set(id, null);
		}
	}

	public IRouter getOrCreateRouter(UUID UUid, Level level, BlockPos pos) {
		IRouter r;
		int id = getIDforUUID(UUid);
		if (id > 0) {
			getRouter(id);
		}
		Identifier dimId = level.dimension().identifier();
		synchronized (routersServer) {
			for (IRouter r2 : routersServer) {
				if (r2 != null && r2.isAt(dimId, pos)) {
					return r2;
				}
			}
			final ServerRouter serverRouter = new ServerRouter(UUid, dimId, pos);

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
		return r;
	}

	/**
	 * Whether a server router exists under {@code id}, without bounds checking: an id outside the
	 * list throws.
	 */
	public boolean isRouterUnsafe(int id) {
		return routersServer.get(id) != null;
	}

	public boolean hasChannelConnection(IRouter router) {
		return channelConnectedPipes.stream()
				.filter(con -> con.routers.size() > 1)
				.anyMatch(con -> con.routers.contains(router.getSimpleID()));
	}

	public boolean addChannelConnection(UUID ident, IRouter router) {
		if (!(router instanceof ServerRouter)) {
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

	public void removeChannelConnection(IRouter router) {
		if (!(router instanceof ServerRouter)) {
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

	public void add(LogisticsSecurityBlockEntity tile) {
		security.put(tile, null);
		UUID secId = tile.getSecId();
		if (secId != null && tile.isAuthorized()) {
			authorizeUUID(secId);
		}
	}

	public @Nullable LogisticsSecurityBlockEntity getStation(@Nullable UUID id) {
		if (id == null) {
			return null;
		}
		return security.keySet().stream()
			.filter(blockEntity -> id.equals(blockEntity.getSecId()))
			.findFirst()
			.orElse(null);
	}

	public void remove(LogisticsSecurityBlockEntity tile) {
		security.remove(tile);
		UUID secId = tile.getSecId();
		if (secId != null) {
			deauthorizeUUID(secId);
		}
	}

	public void dimensionUnloaded(Identifier dim) {
		synchronized (routersServer) {
			routersServer.stream().filter(r -> r != null && r.isInDim(dim)).forEach(r -> {
				r.clearPipeCache();
				r.clearInterests();
			});
		}
	}

	public void deauthorizeUUID(UUID id) {
		authorized.remove(id.toString());
		sendClientAuthorizationList();
	}

	public void authorizeUUID(UUID id) {
		if (!authorized.contains(id.toString())) {
			authorized.add(id.toString());
		}
		sendClientAuthorizationList();
	}

	public boolean isAuthorized(@Nullable UUID id) {
		if (authorized.isEmpty() || id == null) {
			return false;
		}
		return authorized.contains(id.toString());
	}

	public boolean isAuthorized(String id) {
		if (authorized.isEmpty()) {
			return false;
		}
		return authorized.contains(id);
	}

	public void sendClientAuthorizationList() {
		PacketDistributor.sendToAllPlayers(new SecurityAuthorizedListMessage(List.copyOf(authorized)));
	}

	public void sendClientAuthorizationList(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new SecurityAuthorizedListMessage(List.copyOf(authorized)));
		}
	}

	public void printAllRouters() {
		routersServer.stream().filter(router -> router != null).forEach(router -> LogisticsPipes.LOG.info("{}", router));
	}
}
