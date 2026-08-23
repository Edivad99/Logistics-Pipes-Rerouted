package logisticspipes.proxy;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import com.google.common.collect.Maps;

import logisticspipes.LogisticsEventListener;
import logisticspipes.proxy.side.ClientProxy;
import logisticspipes.proxy.side.ServerProxy;
import logisticspipes.world.item.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.entity.FakePlayerLP;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.routing.debug.RoutingTableDebugUpdateThread;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.ticks.RoutingTableUpdateThread;
import logisticspipes.utils.PlayerCollectionList;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class MainProxy {

	private MainProxy() {}

	/**
	 * Side-specific proxy: ClientProxy on client dist, ServerProxy on dedicated server.
	 * Replaces 1.12.2 {@code @SidedProxy} annotation.
	 */
	// NeoForge 1.20.1: DistExecutor removed — use FMLEnvironment.dist check
    @Deprecated(forRemoval = true)
	public static IProxy proxy = FMLEnvironment.dist.isClient()
			? new logisticspipes.proxy.side.ClientProxy()
			: new logisticspipes.proxy.side.ServerProxy();

    /**
     * ClientProxy is {@code }, so the RuntimeDistCleaner refuses to load it
     * on a dedicated server. Holding it in a nested class defers that load until the first
     * {@code getProxy(true)} call, which only ever happens in client-side code.
     */
    private static final class ClientProxyHolder {
        private static final ClientProxy INSTANCE = new ClientProxy();
    }

    private static final ServerProxy serverProxy = new ServerProxy();

    public static IProxy getProxy(boolean client) {
        if (client) {
            return ClientProxyHolder.INSTANCE;
        }
        return serverProxy;
    }


	@Getter
	private static int globalTick;

	private static final WeakHashMap<Thread, LogicalSide> threadSideMap = new WeakHashMap<>();
	private static final Map<ResourceKey<Level>, FakePlayerLP> fakePlayers = Maps.newHashMap();

	// ── Side detection ────────────────────────────────────────────────────────

	private static LogicalSide getEffectiveSide() {
		Thread thr = Thread.currentThread();
		if (MainProxy.threadSideMap.containsKey(thr)) {
			return MainProxy.threadSideMap.get(thr);
		}
		LogicalSide side = MainProxy.getEffectiveSide(thr);
		if (MainProxy.threadSideMap.size() > 50) {
			MainProxy.threadSideMap.clear();
		}
		MainProxy.threadSideMap.put(thr, side);
		return side;
	}

	private static LogicalSide getEffectiveSide(Thread thr) {
		if (thr.getName().equals("Server thread")
				|| (thr instanceof RoutingTableUpdateThread)
				|| (thr instanceof RoutingTableDebugUpdateThread)) {
			return LogicalSide.SERVER;
		}
		// ComputerCraft Lua-thread check removed — CC has no 1.20.1 port (former dummy always returned false).
		return LogicalSide.CLIENT;
	}

	/** Use {@link #isClient(Level)} when a level is available; this thread-based fallback is slow. */
	@Deprecated
	public static boolean isClient() {
		return MainProxy.getEffectiveSide() == LogicalSide.CLIENT;
	}

	/** Use {@link #isServer(Level)} when a level is available; this thread-based fallback is slow. */
	@Deprecated
	public static boolean isServer() {
		return MainProxy.getEffectiveSide() == LogicalSide.SERVER;
	}

	public static boolean isClient(@Nullable Level level) {
		// Mirror isServer(Level): fall back to thread detection when no level is available
		// (e.g. a pipe queried before its container is bound).
		if (level == null) {
            return MainProxy.getEffectiveSide() == LogicalSide.CLIENT;
        }
		return level.isClientSide;
	}

	public static boolean isServer(@Nullable Level level) {
		if (level == null) {
            return MainProxy.getEffectiveSide() == LogicalSide.SERVER;
        }
		return !level.isClientSide;
	}

	/**
	 * Accepts any {@link LevelAccessor} (e.g. from {@code BlockEvent.getLevel()}).
	 * Falls back to thread detection if the accessor is not a full {@link Level}.
	 */
	public static boolean isServer(@Nullable LevelAccessor levelAccessor) {
		if (levelAccessor instanceof Level level) {
			return !level.isClientSide;
		}
		return MainProxy.isServer();
	}

	public static boolean isClient(@Nullable LevelAccessor levelAccessor) {
		if (levelAccessor instanceof Level level) {
			return level.isClientSide;
		}
		return MainProxy.isClient();
	}

	public static void runOnServer(@Nullable LevelAccessor level, Supplier<Runnable> runnableConsumer) {
		if (isServer(level)) runnableConsumer.get().run();
	}

	public static void runOnClient(@Nullable LevelAccessor level, Supplier<Runnable> runnableConsumer) {
		if (isClient(level)) runnableConsumer.get().run();
	}

	// ── Networking ────────────────────────────────────────────────────────────

	/** Sends a packet from the client to the server. */
	public static void sendPacketToServer(ModernPacket packet) {
		if (MainProxy.isServer()) {
			LogisticsPipes.LOG.error("sendPacketToServer called server-side!");
			return;
		}
		PacketHandler.sendToServer(packet);
	}

	/** Sends a packet from the server to a specific player. */
	public static void sendPacketToPlayer(ModernPacket packet, Player player) {
		if (!MainProxy.isServer()) {
			LogisticsPipes.LOG.error("sendPacketToPlayer called client-side!");
			return;
		}
		PacketHandler.sendToPlayer(packet, player);
	}

	// ── Chunk-watch / broadcast helpers ──────────────────────────────────────

	public static boolean isAnyoneWatching(BlockPos pos, int dimensionID) {
		ChunkPos chunkPos = new ChunkPos(pos);
		PlayerCollectionList list = LogisticsEventListener.watcherList.get(chunkPos);
		return list != null && !list.isEmpty();
	}

	public static boolean isAnyoneWatching(int X, int Z, int dimensionID) {
		ChunkPos chunkPos = new ChunkPos(SectionPos.blockToSectionCoord(X), SectionPos.blockToSectionCoord(Z));
		PlayerCollectionList list = LogisticsEventListener.watcherList.get(chunkPos);
		return list != null && !list.isEmpty();
	}

	public static void sendPacketToAllWatchingChunk(@Nullable LogisticsModule module, ModernPacket packet) {
		if (module == null || module.getBlockPos() == null) return;
		ChunkPos chunkPos = new ChunkPos(module.getBlockPos());
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	public static void sendPacketToAllWatchingChunk(@Nullable BlockEntity tile, ModernPacket packet) {
		if (tile == null) return;
		Level lvl = tile.getLevel();
		if (lvl instanceof ServerLevel serverLevel) {
			// The chunk position is derived from the block position rather than by asking the
			// level for the chunk: getChunkAt() is a *blocking full-status chunk load*, and this
			// runs whenever a pipe notifies its watchers. During shutdown that re-requests the
			// promotion of an already-unloading chunk, leaving ChunkHolder.fullChunkFuture — and
			// therefore saveSync — permanently incomplete. ChunkMap.processUnloads then busy-
			// retries scheduleUnload forever, because only the server thread could complete that
			// future and it is the thread stuck in the retry loop. The game hangs on
			// "Saving world" at 100% CPU, and only for worlds containing pipes.
			PacketDistributor.sendToPlayersTrackingChunk(
					serverLevel,
					new ChunkPos(tile.getBlockPos()),
					PacketHandler.buildPayloadPublic(packet)
			);
			return;
		}
		ChunkPos chunkPos = new ChunkPos(tile.getBlockPos());
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	public static void sendPacketToAllWatchingChunk(int X, int Z, int dimensionId, ModernPacket packet) {
		ChunkPos chunkPos = new ChunkPos(SectionPos.blockToSectionCoord(X), SectionPos.blockToSectionCoord(Z));
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	private static void sendPacketToChunkWatchers(ChunkPos chunkPos, ModernPacket packet) {
		PlayerCollectionList list = logisticspipes.LogisticsEventListener.watcherList.get(chunkPos);
		if (list != null) {
			list.players().forEach(p -> sendPacketToPlayer(packet, p));
		}
	}

	public static void sendToPlayerList(ModernPacket packet, PlayerCollectionList players) {
		players.players().forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToPlayerList(ModernPacket packet, Iterable<Player> players) {
		players.forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToPlayerList(ModernPacket packet, Stream<Player> players) {
		players.forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToAllPlayers(ModernPacket packet) {
		if (!MainProxy.isServer()) {
			LogisticsPipes.LOG.error("sendToAllPlayers called client-side!");
			return;
		}
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		for (ServerLevel level : server.getAllLevels()) {
			for (Player player : level.players()) {
				MainProxy.sendPacketToPlayer(packet, player);
			}
		}
	}

	// ── Fake player ──────────────────────────────────────────────────────────

	@Nullable
	public static FakePlayer getFakePlayer(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) return null;
		ResourceKey<Level> dim = level.dimension();
		if (fakePlayers.containsKey(dim)) return fakePlayers.get(dim);
		FakePlayerLP fp = new FakePlayerLP(serverLevel);
		fakePlayers.put(dim, fp);
		return fp;
	}

	// ── Misc ─────────────────────────────────────────────────────────────────

	public static void addTick() {
		MainProxy.globalTick++;
	}

	public static ItemEntity dropItems(Level level, ItemStack stack, int xCoord, int yCoord, int zCoord) {
		ItemEntity item = new ItemEntity(level, xCoord, yCoord, zCoord, stack);
		level.addFreshEntity(item);
		return item;
	}

	public static boolean checkPipesConnections(BlockEntity from, BlockEntity to, Direction way) {
		return MainProxy.checkPipesConnections(from, to, way, false);
	}

	public static boolean checkPipesConnections(@Nullable BlockEntity from, @Nullable BlockEntity to, Direction way, boolean ignoreSystemDisconnection) {
		if (from == null || to == null) return false;
		IPipeInformationProvider fromInfo = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(from);
		IPipeInformationProvider toInfo   = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(to);
		if (fromInfo == null && toInfo == null) return false;
		if (fromInfo != null && !fromInfo.canConnect(to, way, ignoreSystemDisconnection)) return false;
		if (toInfo   != null) return toInfo.canConnect(from, way.getOpposite(), ignoreSystemDisconnection);
		return true;
	}

	public static boolean isPipeControllerEquipped(@Nullable Player player) {
		return player != null &&
				!player.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() &&
				player.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.PIPE_CONTROLLER.get());
	}

	@SubscribeEvent
	public static void onWorldUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof Level level) {
			fakePlayers.keySet().removeIf(key -> key.equals(level.dimension()));
		}
	}

	private static boolean needsToBeCompressed(ModernPacket packet) {
		return false;
	}
}
