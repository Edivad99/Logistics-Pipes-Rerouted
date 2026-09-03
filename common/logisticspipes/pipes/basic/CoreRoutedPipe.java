/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import net.minecraft.CrashReportCategory;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import kotlin.Unit;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.IClientState;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.IQueueCCEvent;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.IWatchingHandler;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.logisticspipes.ITrackStatistics;
import logisticspipes.logisticspipes.PipeTransportLayer;
import logisticspipes.logisticspipes.RouteLayer;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.guis.pipe.NormalOrdererGui;
import logisticspipes.network.guis.pipe.PipeController;
import logisticspipes.network.to_client.pipe.PipeSignTypesMessage;
import logisticspipes.network.to_client.pipe.PipeStatsMessage;
import logisticspipes.network.to_server.pipe.RequestPipeSignsMessage;
import logisticspipes.network.to_server.pipe.RequestRoutingLasersMessage;
import logisticspipes.particle.Particles;
import logisticspipes.particle.PipeFXRenderHandler;
import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.CCConstants;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCDirectCall;
import logisticspipes.proxy.computers.interfaces.CCSecurtiyCheck;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.routing.order.LogisticsOrderManager;
import logisticspipes.security.PermissionException;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.DirectionUtil;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import logisticspipes.world.item.ItemPipeSignCreator;
import logisticspipes.world.item.LPItems;
import network.rs485.logisticspipes.connection.Adjacent;
import network.rs485.logisticspipes.connection.AdjacentFactory;
import network.rs485.logisticspipes.connection.NoAdjacent;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.property.UtilKt;

@CCType(name = "LogisticsPipes:Normal")
public abstract class CoreRoutedPipe extends CoreUnroutedPipe
		implements IClientState, IRequestItems, ITrackStatistics, IWorldProvider, IWatchingHandler, IPipeServiceProvider, IQueueCCEvent, ILPPositionProvider {

	private static int pipecount = 0;
	public final PlayerCollectionList watchers = new PlayerCollectionList();
	protected final PriorityBlockingQueue<ItemRoutingInformation> inTransitToMe = new PriorityBlockingQueue<>(10,
			new ItemRoutingInformation.DelayComparator());
	protected final LinkedList<Triplet<IRoutedItem, Direction, ItemSendMode>> sendQueue = new LinkedList<>();
	protected final Map<ItemIdentifier, Queue<Pair<Integer, ItemRoutingInformation>>> queuedDataForUnroutedItems = Collections.synchronizedMap(new TreeMap<>());
	public boolean textureBufferPowered;
	public long delayTo = 0;
	public int repeatFor = 0;
	public long stat_session_sent;
	public long stat_session_received;
	public long stat_session_relayed;
	public long stat_lifetime_sent;
	public long stat_lifetime_received;
	public long stat_lifetime_relayed;
	public int server_routing_table_size = 0;
	protected boolean stillNeedReplace = true;
    @Nullable
	protected IRouter router;
    @Nullable
	protected String routerId;
	protected final Object routerIdLock = new Object();
	protected int delayOffset;
	protected boolean initialInit = true;
    @Nullable
	protected RouteLayer routeLayer;
    @Nullable
	protected TransportLayer transportLayer;

	final protected UpgradeManager upgradeManager = new UpgradeManager(this);

    @Nullable
	protected LogisticsItemOrderManager orderItemManager = null;
	protected int throttleTime = 20;
	protected IPipeSign[] signItem = new IPipeSign[6];
	private boolean recheckConnections = false;
	@Setter
    @Getter
    private boolean enabled = true;
	private boolean preventRemove = false;
	private boolean destroyByPlayer = false;
	private final PowerSupplierHandler powerHandler = new PowerSupplierHandler(this);
	@Getter
	private final List<IOrderInfoProvider> clientSideOrderManager = new ArrayList<>();
	private int throttleTimeLeft;
	private final int[] queuedParticles = new int[Particles.values().length];
	private boolean hasQueuedParticles = false;
	private boolean isOpaqueClientSide = false;

	/** Caches adjacent state, only on Side.SERVER */
	private Adjacent adjacent = NoAdjacent.INSTANCE;

	/**
	 * @return the adjacent cache directly.
	 */
	protected Adjacent getAdjacent() {
		return adjacent;
	}

	/**
	 * Returns all adjacents on a regular routed pipe.
	 */
	@Override
	public Adjacent getAvailableAdjacent() {
		return getAdjacent();
	}

	@Nullable
	@Override
	public Direction getPointedOrientation() {
		// from IPipeServiceProvider, overridden in the PipeLogisticsChassis
		return null;
	}

	/**
	 * Re-creates adjacent cache.
	 */
	protected void updateAdjacentCache() {
		adjacent = AdjacentFactory.INSTANCE.createAdjacentCache(this);
	}

    @Nullable
	private CacheHolder cacheHolder;

	public CoreRoutedPipe(Item item) {
		this(new PipeTransportLogistics(true), item);
	}

	public CoreRoutedPipe(PipeTransportLogistics transport, Item item) {
		super(transport, item);

		CoreRoutedPipe.pipecount++;

	}

	@Override
	public void initialize() {
		super.initialize();
		throttleTimeLeft = 20 + new Random().nextInt(LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt());
		//Roughly spread pipe updates throughout the frequency, no need to maintain balance
		delayOffset = CoreRoutedPipe.pipecount % LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt();
	}

	@Override
	public void markTileDirty() {
		if (container != null) container.setChanged();
	}

	public RouteLayer getRouteLayer() {
		if (routeLayer == null) {
			routeLayer = new RouteLayer(getRouter(), getTransportLayer(), this);
		}
		return routeLayer;
	}

	public TransportLayer getTransportLayer() {
		if (transportLayer == null) {
			transportLayer = new PipeTransportLayer(this, this, getRouter());
		}
		return transportLayer;
	}

	@Override
	public ISlotUpgradeManager getUpgradeManager(ModulePositionType slot, int positionInt) {
		return upgradeManager;
	}

	@Override
	public IPipeUpgradeManager getUpgradeManager() {
		return upgradeManager;
	}

	public UpgradeManager getOriginalUpgradeManager() {
		return upgradeManager;
	}

	@Override
	public void queueRoutedItem(IRoutedItem routedItem, Direction from) {
        Objects.requireNonNull(from);
		sendQueue.addLast(new Triplet<>(routedItem, from, ItemSendMode.Normal));
		sendQueueChanged(false);
	}

	public void queueRoutedItem(IRoutedItem routedItem, Direction from, ItemSendMode mode) {
        Objects.requireNonNull(from);
		sendQueue.addLast(new Triplet<>(routedItem, from, mode));
		sendQueueChanged(false);
	}

	/**
	 * @param force == true never delegates to a thread
	 * @return number of things sent.
	 */
	public int sendQueueChanged(boolean force) {
		return 0;
	}

	private void sendRoutedItem(IRoutedItem routedItem, Direction from) {
        Objects.requireNonNull(from);

		transport.injectItem(routedItem, from.getOpposite());

		IRouter r = SimpleServiceLocator.routerManager.getServerRouter(routedItem.getDestination());
		if (r != null) {
			CoreRoutedPipe pipe = r.getCachedPipe();
			if (pipe != null) {
				pipe.notifyOfSend(routedItem.getInfo());
			}
			// If the destination pipe's chunk is currently unloaded, getCachedPipe() returns null.
			// That's fine: the item still travels through the loaded portion of the network, and
			// when it arrives at the destination pipe's BlockEntity (which reloads with its chunk)
			// normal delivery takes over. We just skip the in-transit bookkeeping for that pipe —
			// the send queue's timeout (see _inTransitToMe prune in updateEntity) catches any
			// permanently-orphaned items.
		} // should not be able to send to a non-existing router
		// router.startTrackingRoutedItem((RoutedEntityItem) routedItem.getTravelingItem());
		spawnParticle(Particles.ORANGE_SPARKLE, 2);
		stat_lifetime_sent++;
		stat_session_sent++;
		updateStats();
	}

	private void notifyOfSend(ItemRoutingInformation routedItem) {
		inTransitToMe.add(routedItem);
		//LogisticsPipes.log.info("Sending: "+routedItem.getIDStack().getItem().getFriendlyName());
	}

	public void notifyOfReroute(ItemRoutingInformation routedItem) {
		inTransitToMe.remove(routedItem);
	}

	//When Recreating the Item from the TE version we have the same hashCode but a different instance so we need to refresh this
	public void refreshItem(ItemRoutingInformation routedItem) {
		if (inTransitToMe.contains(routedItem)) {
			inTransitToMe.remove(routedItem);
			inTransitToMe.add(routedItem);
		}
	}

	public abstract ItemSendMode getItemSendMode();

	/**
	 * Designed to help protect against routing loops - if both pipes are on the same block
	 *
	 * @return boolean indicating if other and this are attached to the same inventory.
	 */
	public boolean isOnSameContainer(CoreRoutedPipe other) {
		Set<BlockPos> myPositions = adjacent.connectedPos().keySet();
		Set<BlockPos> otherPositions = other.adjacent.connectedPos().keySet();

		// Direct overlap: both pipes adjacent to the same block
		if (myPositions.stream().anyMatch(otherPositions::contains)) {
			return true;
		}

		// Double-chest: one pipe is adjacent to the left half, the other to the right half.
		// ChestBlock.getConnectedDirection() points from one half to the partner half.
		Level level = getWorld();
		if (level == null) return false;
		for (BlockPos pos : myPositions) {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof ChestBlock)) continue;
			if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) continue;
			BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
			if (otherPositions.contains(partner)) return true;
		}
		return false;
	}

	/***
	 * first tick just create a router and do nothing.
	 */
	public void firstInitialiseTick() {
		getRouter();
		if (MainProxy.isClient(getWorld())) {
			ClientPacketDistributor.sendToServer(new RequestPipeSignsMessage(getPos()));
		}
	}

	/***
	 * Only Called Server Side Only Called when the pipe is enabled
	 */
	public void enabledUpdateEntity() {
		powerHandler.update();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				signItem[i].updateServerSide();
			}
		}
	}

	/***
	 * Called Server and Client Side Called every tick
	 */
	public void ignoreDisableUpdateEntity() {}

	@Override
	public final void updateEntity() {
		debug.tick();
		spawnParticleTick();
		if (stillNeedReplace) {
			stillNeedReplace = false;
			//BlockState state = getWorld().getBlockState(getPos());
			//getWorld().updateNeighborsAt(getPos(), state == null ? null : state.getBlock());
			/* TravelingItems are just held by a pipe, they don't need to know their world
			 * for(Triplet<IRoutedItem, Direction, ItemSendMode> item : _sendQueue) {
				//assign world to any entityitem we created in readfromnbt
				item.getValue1().getTravelingItem().setWorld(getWorld());
			}*/
			//first tick just create a router and do nothing.
			firstInitialiseTick();
			return;
		}
		if (repeatFor > 0) {
			if (delayTo < System.currentTimeMillis()) {
				delayTo = System.currentTimeMillis() + 200;
				repeatFor--;
				getWorld().updateNeighborsAt(getPos(), getWorld().getBlockState(getPos()).getBlock());
			}
		}

		// remove old items _inTransit -- these should have arrived, but have probably been lost instead. In either case, it will allow a re-send so that another attempt to re-fill the inventory can be made.
		while (inTransitToMe.peek() != null && inTransitToMe.peek().getTickToTimeOut() <= 0) {
			final ItemRoutingInformation polledInfo = inTransitToMe.poll();
			if (polledInfo != null) {
				if (LogisticsPipes.isDEBUG()) {
					LogisticsPipes.LOG.info("Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
				}
				debug.log("Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
			}
		}
		//update router before ticking logic/transport
		final boolean doFullRefresh =
				getWorld().getGameTime() % LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt() == delayOffset
				|| initialInit || recheckConnections;
		if (doFullRefresh) {
			// update adjacent cache first, so interests can be gathered correctly
			// in getRouter().update(…) below
			updateAdjacentCache();
		}
		getRouter().update(doFullRefresh, this);
		recheckConnections = false;
		getOriginalUpgradeManager().securityTick();
		super.updateEntity();

		if (isNthTick(200)) {
			getCacheHolder().trigger(null);
		}

		// from BaseRoutingLogic
		if (--throttleTimeLeft <= 0) {
			throttledUpdateEntity();
			throttleTimeLeft = throttleTime;
		}

		ignoreDisableUpdateEntity();
		initialInit = false;
		if (!sendQueue.isEmpty()) {
            switch (getItemSendMode()) {
                case Normal -> {
                    Triplet<IRoutedItem, Direction, ItemSendMode> itemToSend = sendQueue.getFirst();
                    sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
                    sendQueue.removeFirst();
                    for (int i = 0; i < 16 && !sendQueue.isEmpty() && sendQueue.getFirst().getValue3() == ItemSendMode.Fast; i++) {
                        if (!sendQueue.isEmpty()) {
                            itemToSend = sendQueue.getFirst();
                            sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
                            sendQueue.removeFirst();
                        }
                    }
                    sendQueueChanged(false);
                }
                case Fast -> {
                    for (int i = 0; i < 16; i++) {
                        if (!sendQueue.isEmpty()) {
                            Triplet<IRoutedItem, Direction, ItemSendMode> itemToSend = sendQueue.getFirst();
                            sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
                            sendQueue.removeFirst();
                        }
                    }
                    sendQueueChanged(false);
                }
            }
		}
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		checkTexturePowered();
		if (!isEnabled()) {
			return;
		}
		enabledUpdateEntity();
		if (getLogisticsModule() == null) {
			return;
		}
		getLogisticsModule().tick();
	}

	protected void onAllowedRemoval() {}

	// From BaseRoutingLogic
	public void throttledUpdateEntity() {}

	protected void delayThrottle() {
		//delay 6(+1) ticks to prevent suppliers from ticking between a item arriving at them and the item hitting their adj. inv
		if (throttleTimeLeft < 7) {
			throttleTimeLeft = 7;
		}
	}

	@Override
	public boolean isNthTick(int n) {
		return ((getWorld().getGameTime() + delayOffset) % n == 0);
	}

	private void doDebugStuff(Player entityplayer) {
		//entityplayer.level().setWorldTime(4951);
		if (!MainProxy.isServer(entityplayer.level())) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		ServerRouter router = (ServerRouter) getRouter();

		sb.append("***\n");
		sb.append("---------Interests---------------\n");
		ServerRouter.forEachGlobalSpecificInterest((itemIdentifier, serverRouters) -> {
			sb.append(itemIdentifier.getFriendlyName()).append(":");
			for (IRouter j : serverRouters) {
				sb.append(j.getSimpleID()).append(",");
			}
			sb.append('\n');
		});

		sb.append("ALL ITEMS:");
		for (IRouter j : ServerRouter.getInterestedInGeneral()) {
			sb.append(j.getSimpleID()).append(",");
		}
		sb.append('\n');

		sb.append(router).append('\n');
		sb.append("---------CONNECTED TO---------------\n");
		for (CoreRoutedPipe adj : router.adjacent.keySet()) {
			sb.append(adj.getRouter().getSimpleID()).append('\n');
		}
		sb.append('\n');
		sb.append("========DISTANCE TABLE==============\n");
		for (ExitRoute n : router.getIRoutersByCost()) {
			sb.append(n.destination.getSimpleID())
					.append(" @ ")
					.append(n.distanceToDestination)
					.append(" -> ")
					.append(n.connectionDetails)
					.append("(")
					.append(n.destination.getId())
					.append(")")
					.append('\n');
		}
		sb.append('\n');
		sb.append("*******EXIT ROUTE TABLE*************\n");
		List<List<ExitRoute>> table = router.getRouteTable();
		for (int i = 0; i < table.size(); i++) {
			if (table.get(i) != null) {
				if (table.get(i).size() > 0) {
					sb.append(i).append(" -> ").append(table.get(i).get(0).destination.getSimpleID()).append('\n');
					for (ExitRoute route : table.get(i)) {
						sb.append("\t\t via ").append(route.exitOrientation).append("(").append(route.distanceToDestination).append(" distance)").append('\n');
					}
				}
			}
		}
		sb.append('\n');
		sb.append("++++++++++CONNECTIONS+++++++++++++++\n");
		sb.append(Arrays.toString(Direction.values())).append('\n');
		sb.append(Arrays.toString(router.sideDisconnected)).append('\n');
		if (container != null) {
			sb.append(Arrays.toString(container.pipeConnectionsBuffer)).append('\n');
		}
		sb.append("+++++++++++++ADJACENT+++++++++++++++\n");
		sb.append(adjacent).append('\n');
		sb.append("pointing: ").append(getPointedOrientation()).append('\n');
		sb.append("~~~~~~~~~~~~~~~POWER~~~~~~~~~~~~~~~~\n");
		sb.append(router.getPowerProvider()).append('\n');
		sb.append("~~~~~~~~~~~SUBSYSTEMPOWER~~~~~~~~~~~\n");
		sb.append(router.getSubSystemPowerProvider()).append('\n');
		if (orderItemManager != null) {
			sb.append("################ORDERDUMP#################\n");
			orderItemManager.dump(sb);
		}
		sb.append("################END#################\n");
		refreshConnectionAndRender(true);
		LogisticsPipes.LOG.info("{}", sb);
		router.CreateRouteTable(Integer.MAX_VALUE);
	}

	// end FromBaseRoutingLogic

	@Override
	public final void onBlockRemoval() {
		try {
			onAllowedRemoval();
			super.onBlockRemoval();
			//Just in case
			CoreRoutedPipe.pipecount = Math.max(CoreRoutedPipe.pipecount - 1, 0);

            transport.dropBuffer();
            getOriginalUpgradeManager().dropUpgrades();
		} catch (Exception e) {
			LogisticsPipes.LOG.error("Exception during pipe teardown at ({}, {}, {})", getX(), getY(), getZ(), e);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (router != null) {
			router.destroy();
			router = null;
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (router != null) {
			router.clearPipeCache();
			router.clearInterests();
		}
	}

	public void checkTexturePowered() {
		if (LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean()) {
			return;
		}
		if (!isNthTick(10)) {
			return;
		}
		if (stillNeedReplace || initialInit || router == null) {
			return;
		}
		boolean flag;
		if ((flag = canUseEnergy(1)) != textureBufferPowered) {
			textureBufferPowered = flag;
			refreshRender(false);
			spawnParticle(Particles.RED_SPARKLE, 3);
		}
	}

	@Override
	public int getTextureIndex() {
		return getCenterTexture().newTexture;
	}

	public abstract TextureType getCenterTexture();

	public TextureType getTextureType(@Nullable Direction connection) {
		if (stillNeedReplace || initialInit) {
			return getCenterTexture();
		}

		if (connection == null) {
			return getCenterTexture();
		} else if ((router != null) && getRouter().isRoutedExit(connection)) {
			return getRoutedTexture(connection);
		} else {
			TextureType texture = getNonRoutedTexture(connection);
			if (this.getUpgradeManager().hasRFPowerSupplierUpgrade() || this.getUpgradeManager().getIC2PowerLevel() > 0) {
				if (texture.fileName.equals(Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_LIQUID_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_LIQUID_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_POWERED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_POWERED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE;
				} else {
					LogisticsPipes.LOG.warn("Unknown texture to power: {} class={} connection={}", texture.fileName, this.getClass(), connection);
				}
			}
			return texture;
		}
	}

	public TextureType getRoutedTexture(Direction connection) {
		if (getRouter().isSubPoweredExit(connection)) {
			return Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE;
		} else {
			return Textures.LOGISTICSPIPE_ROUTED_TEXTURE;
		}
	}

	public TextureType getNonRoutedTexture(Direction connection) {
		if (isPowerProvider(connection)) {
			return Textures.LOGISTICSPIPE_POWERED_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE;
	}

    @Override
	public void spawnParticle(Particles particle, int amount) {
		if (!LPConfigs.COMMON.ENABLE_PARTICLE_FX.getAsBoolean()) {
			return;
		}
		queuedParticles[particle.ordinal()] += amount;
		hasQueuedParticles = true;
	}

	private void spawnParticleTick() {
		if (!hasQueuedParticles) {
			return;
		}
        if (getWorld() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < this.queuedParticles.length; i++) {
                if (this.queuedParticles[i] > 0) {
                    var amount = this.queuedParticles[i];
                    serverLevel.sendParticles(Particles.values()[i].getSparkleFXParticleOptions(amount),
                        getX(), getY(), getZ(), amount, 0, 0, 0, 1);
                }
            }
        } else if (getWorld() instanceof ClientLevel) {
            if (Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST) {
                for (int i = 0; i < queuedParticles.length; i++) {
                    if (this.queuedParticles[i] > 0) {
                        PipeFXRenderHandler.spawnGenericParticle(Particles.values()[i],
                            getX(), getY(), getZ(), queuedParticles[i]);
                    }
                }
            }
        }
		Arrays.fill(queuedParticles, 0);
		hasQueuedParticles = false;
	}

	protected boolean isPowerProvider(@Nullable Direction direction) {
		if (direction == null) return false;
		BlockEntity tilePipe = container.getTile(direction);
		if (tilePipe == null || !container.canPipeConnect(tilePipe, direction)) {
			return false;
		}

		return tilePipe instanceof ILogisticsPowerProvider || tilePipe instanceof ISubSystemPowerProvider;
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);

		synchronized (routerIdLock) {
			if (routerId == null || routerId.isEmpty()) {
				if (router != null) {
					routerId = router.getId().toString();
				} else {
					routerId = UUID.randomUUID().toString();
				}
			}
		}
		output.putString("routerId", routerId);
		output.putLong("stat_lifetime_sent", stat_lifetime_sent);
		output.putLong("stat_lifetime_received", stat_lifetime_received);
		output.putLong("stat_lifetime_relayed", stat_lifetime_relayed);
		if (getLogisticsModule() != null) {
			getLogisticsModule().serialize(output);
		}
		output.putChild("upgradeManager", upgradeManager);
		output.putChild("powerHandler", powerHandler);

		ValueOutput.ValueOutputList sendqueue = output.childrenList("sendqueue");
		for (Triplet<IRoutedItem, Direction, ItemSendMode> p : sendQueue) {
			ValueOutput tagentry = sendqueue.addChild();
			p.getValue1().serialize(tagentry.child("entityitem"));
			tagentry.putByte("from", (byte) (p.getValue2().ordinal()));
			tagentry.putByte("mode", (byte) (p.getValue3().ordinal()));
		}

		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				output.putBoolean("PipeSign_" + i, true);
				int signType = -1;
				List<Class<? extends IPipeSign>> typeClasses = ItemPipeSignCreator.signTypes;
				for (int j = 0; j < typeClasses.size(); j++) {
					if (typeClasses.get(j) == signItem[i].getClass()) {
						signType = j;
						break;
					}
				}
				output.putInt("PipeSign_" + i + "_type", signType);
				output.putChild("PipeSign_" + i + "_tags", signItem[i]);
			} else {
				output.putBoolean("PipeSign_" + i, false);
			}
		}

		if (this instanceof PropertyHolder) {
			PropertyHolder.serialize(output, (PropertyHolder) this);
		}
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);

		synchronized (routerIdLock) {
			routerId = input.getStringOr("routerId", "");
		}

		stat_lifetime_sent = input.getLongOr("stat_lifetime_sent", 0L);
		stat_lifetime_received = input.getLongOr("stat_lifetime_received", 0L);
		stat_lifetime_relayed = input.getLongOr("stat_lifetime_relayed", 0L);
		if (getLogisticsModule() != null) {
			getLogisticsModule().deserialize(input);
		}
		upgradeManager.deserialize(input.childOrEmpty("upgradeManager"));
		powerHandler.deserialize(input.childOrEmpty("powerHandler"));

		sendQueue.clear();
		for (ValueInput tagentry : input.childrenListOrEmpty("sendqueue")) {
			LPTravelingItemServer item = new LPTravelingItemServer(tagentry.childOrEmpty("entityitem"));
			Direction from = Direction.values()[tagentry.getByteOr("from", (byte) 0)];
			ItemSendMode mode = ItemSendMode.values()[tagentry.getByteOr("mode", (byte) 0)];
			sendQueue.add(new Triplet<>(item, from, mode));
		}
		for (int i = 0; i < 6; i++) {
			if (input.getBooleanOr("PipeSign_" + i, false)) {
				int type = input.getIntOr("PipeSign_" + i + "_type", 0);
				Class<? extends IPipeSign> typeClass = ItemPipeSignCreator.signTypes.get(type);
				try {
					signItem[i] = typeClass.newInstance();
					signItem[i].init(this, DirectionUtil.getOrientation(i));
					signItem[i].deserialize(input.childOrEmpty("PipeSign_" + i + "_tags"));
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}

		if (this instanceof PropertyHolder) {
			PropertyHolder.deserialize(input, (PropertyHolder) this);
		}
	}

	@Override
    public IRouter getRouter() {
		if (stillNeedReplace) {
			LogisticsPipes.LOG.debug("Pipe not ready at ({}, {}, {}, '{}')", this.getX(), this.getY(), this.getZ(),
					getWorld() != null ? getWorld().dimension().identifier().toString() : "unknown");
		}
		if (router == null) {
			synchronized (routerIdLock) {

				UUID routerIntId = null;
				if (routerId != null && !routerId.isEmpty()) {
					routerIntId = UUID.fromString(routerId);
				}
				router = SimpleServiceLocator.routerManager.getOrCreateRouter(routerIntId, getWorld(), getX(), getY(), getZ());
			}
		}
		return router;
	}

    @CCCommand(description = "Returns the Internal LogisticsModule for this pipe")
	public abstract @Nullable LogisticsModule getLogisticsModule();

	@Override
	public final boolean blockActivated(Player entityplayer) {
		if (container == null) return super.blockActivated(entityplayer);
		SecuritySettings settings = null;
		if (MainProxy.isServer(entityplayer.level())) {
			LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
			if (station != null) {
				settings = station.getSecuritySettingsForPlayer(entityplayer, true);
			}
		}

		if (MainProxy.isPipeControllerEquipped(entityplayer)) {
			if (MainProxy.isServer(entityplayer.level())) {
				if (settings == null || settings.openNetworkMonitor) {
					NewGuiHandler.getGui(PipeController.class).setTilePos(container).open(entityplayer);
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		if (handleClick(entityplayer, settings)) {
			return true;
		}

		if (entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
			if (!entityplayer.isCrouching()) {
				return false;
			}
			if (MainProxy.isClient(entityplayer.level())) {
				if (LogisticsHUDRenderer.instance().hasLasers()) {
					LogisticsHUDRenderer.instance().resetLasers();
				} else {
					ClientPacketDistributor.sendToServer(new RequestRoutingLasersMessage(getPos()));
				}
			}
			if (LogisticsPipes.isDEBUG()) {
				doDebugStuff(entityplayer);
			}
			return true;
		}

		if (entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.REMOTE_ORDERER)) {
			if (MainProxy.isServer(entityplayer.level())) {
				if (settings == null || settings.openRequest) {
					NormalOrdererGui gui = NewGuiHandler.getGui(NormalOrdererGui.class);
					gui.setPosX(getX()).setPosY(getY()).setPosZ(getZ());
					gui.setDim(entityplayer.level().dimension().identifier());
					gui.open(entityplayer);
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		if (SimpleServiceLocator.configToolHandler.canWrench(entityplayer, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND), container)) {
			if (MainProxy.isServer(entityplayer.level())) {
				if (settings == null || settings.openGui) {
					final LogisticsModule module = getLogisticsModule();
					if (module instanceof IModuleMenuProvider && entityplayer instanceof ServerPlayer serverPlayer) {
						IModuleMenuProvider.open(serverPlayer, module);
					} else {
						onWrenchClicked(entityplayer);
					}
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
			}
			SimpleServiceLocator.configToolHandler.wrenchUsed(entityplayer, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND), container);
			return true;
		}

		if (!(entityplayer.isCrouching()) && getOriginalUpgradeManager().tryIserting(getWorld(), entityplayer)) {
			return true;
		}

		return super.blockActivated(entityplayer);
	}

	protected boolean handleClick(Player entityplayer, @Nullable SecuritySettings settings) {
		return false;
	}

	public void refreshRender(boolean spawnPart) {
		container.scheduleRenderUpdate();
		if (spawnPart) {
			spawnParticle(Particles.GREEN_SPARKLE, 3);
		}
	}

	public void refreshConnectionAndRender(boolean spawnPart) {
		container.scheduleNeighborChange();
		if (spawnPart) {
			spawnParticle(Particles.GREEN_SPARKLE, 3);
		}
	}

	/* ITrackStatistics */

	@Override
	public void receivedItem(int count) {
		stat_session_received += count;
		stat_lifetime_received += count;
		updateStats();
	}

	@Override
	public void relayedItem(int count) {
		stat_session_relayed += count;
		stat_lifetime_relayed += count;
		updateStats();
	}

	/**
	 * How many items went through a pipe, over one window of time.
	 *
	 * <p>The same three counters are kept twice: once since the server started, once for the
	 * pipe's whole life.
	 */
	public record TrafficCounts(long sent, long received, long relayed) {

		public static final StreamCodec<RegistryFriendlyByteBuf, TrafficCounts> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, TrafficCounts::sent,
						ByteBufCodecs.VAR_LONG, TrafficCounts::received,
						ByteBufCodecs.VAR_LONG, TrafficCounts::relayed,
						TrafficCounts::new);
	}

	public TrafficCounts sessionCounts() {
		return new TrafficCounts(stat_session_sent, stat_session_received, stat_session_relayed);
	}

	public TrafficCounts lifetimeCounts() {
		return new TrafficCounts(stat_lifetime_sent, stat_lifetime_received, stat_lifetime_relayed);
	}

	/** How many other pipes this one can reach. */
	public int routingTableSize() {
		int reachable = 0;
		for (List<ExitRoute> route : getRouter().getRouteTable()) {
			if (route != null && !route.isEmpty()) {
				reachable++;
			}
		}
		return reachable;
	}

	public void applyStats(TrafficCounts session, TrafficCounts lifetime, int routingTableSize) {
		stat_session_sent = session.sent();
		stat_session_received = session.received();
		stat_session_relayed = session.relayed();
		stat_lifetime_sent = lifetime.sent();
		stat_lifetime_received = lifetime.received();
		stat_lifetime_relayed = lifetime.relayed();
		server_routing_table_size = routingTableSize;
	}

	private PipeStatsMessage statsMessage() {
		return new PipeStatsMessage(getPos(), sessionCounts(), lifetimeCounts(), routingTableSize());
	}

	@Override
	public void playerStartWatching(Player player, WatchMode mode) {
		if (mode == WatchMode.GUI) {
			watchers.add(player);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, statsMessage());
			}
		}
	}

	@Override
	public void playerStopWatching(Player player, WatchMode mode) {
		if (mode == WatchMode.GUI) {
			watchers.remove(player);
		}
	}

	public void updateStats() {
		if (!watchers.isEmpty()) {
			watchers.send(statsMessage());
		}
	}

	@Override
	public void itemCouldNotBeSend(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (this instanceof IRequireReliableTransport) {
			((IRequireReliableTransport) this).itemLost(item, info);
		}
	}

	public boolean isLockedExit(Direction orientation) {
		return false;
	}

	public boolean logisticsIsPipeConnected(BlockEntity tile, Direction dir) {
		return false;
	}

	@Override
	public final boolean canPipeConnect(BlockEntity tile, Direction dir) {
		return canPipeConnect(tile, dir, false);
	}

	@Override
	public final boolean canPipeConnect(BlockEntity tile, Direction dir, boolean ignoreSystemDisconnection) {
		Direction side = OrientationsUtil.getOrientationOfTilewithTile(container, tile);
		if (isSideBlocked(side, ignoreSystemDisconnection)) {
			return false;
		}
		return (super.canPipeConnect(tile, dir) || logisticsIsPipeConnected(tile, dir));
	}

	@Override
	public final boolean isSideBlocked(Direction side, boolean ignoreSystemDisconnection) {
		if (getUpgradeManager().isSideDisconnected(side)) {
			return true;
		}
		return !stillNeedReplace && getRouter().isSideDisconnected(side) && !ignoreSystemDisconnection;
	}

	public void connectionUpdate() {
		if (container != null && !stillNeedReplace) {
			if (MainProxy.isClient(getWorld())) throw new IllegalStateException("Wont do connectionUpdate on client-side");
			container.scheduleNeighborChange();
			BlockState state = getWorld().getBlockState(getPos());
			getWorld().updateNeighborsAt(getPos(), state.getBlock());
		}
	}

	public UUID getSecurityID() {
		return getOriginalUpgradeManager().getSecurityID();
	}

	public void insetSecurityID(UUID id) {
		getOriginalUpgradeManager().insetSecurityID(id);
	}

    @Nullable
	public List<Pair<ILogisticsPowerProvider, List<IFilter>>> getRoutedPowerProviders() {
		if (MainProxy.isClient(getWorld())) {
			return null;
		}
		if (stillNeedReplace) {
			return null;
		}
		return getRouter().getPowerProvider();
	}

	/* Power System */

	@Override
	public boolean useEnergy(int amount) {
		return useEnergy(amount, null, true);
	}

	public boolean useEnergy(int amount, boolean sparkles) {
		return useEnergy(amount, null, sparkles);
	}

	@Override
	public boolean canUseEnergy(int amount) {
		return canUseEnergy(amount, null);
	}

	@Override
	public boolean canUseEnergy(int amount, @Nullable List<Object> providersToIgnore) {
		if (MainProxy.isClient(getWorld())) {
			return false;
		}
		if (LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean()) {
			return true;
		}
		if (amount == 0) {
			return true;
		}
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		List<Pair<ILogisticsPowerProvider, List<IFilter>>> list = getRoutedPowerProviders();
		if (list == null) {
			return false;
		}
		outer:
		for (Pair<ILogisticsPowerProvider, List<IFilter>> provider : list) {
			for (IFilter filter : provider.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (provider.getValue1().canUseEnergy(amount, providersToIgnore)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean useEnergy(int amount, @Nullable List<Object> providersToIgnore) {
		return useEnergy(amount, providersToIgnore, false);
	}

	private boolean useEnergy(int amount, @Nullable List<Object> providersToIgnore, boolean sparkles) {
		if (MainProxy.isClient(getWorld())) {
			return false;
		}
		if (LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean()) {
			return true;
		}
		if (amount == 0) {
			return true;
		}
		if (providersToIgnore == null) {
			providersToIgnore = new ArrayList<>();
		}
		if (providersToIgnore.contains(this)) {
			return false;
		}
		providersToIgnore.add(this);
		List<Pair<ILogisticsPowerProvider, List<IFilter>>> list = getRoutedPowerProviders();
		if (list == null) {
			return false;
		}
		outer:
		for (Pair<ILogisticsPowerProvider, List<IFilter>> provider : list) {
			for (IFilter filter : provider.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (provider.getValue1().canUseEnergy(amount, providersToIgnore)) {
				if (provider.getValue1().useEnergy(amount, providersToIgnore)) {
					if (sparkles) {
						int particlecount = amount;
						if (particlecount > 10) {
							particlecount = 10;
						}
						spawnParticle(Particles.GOLD_SPARKLE, particlecount);
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void queueEvent(String event, Object[] arguments) {
		if (container != null) {
			container.queueEvent(event, arguments);
		}
	}

	public boolean stillNeedReplace() {
		return stillNeedReplace;
	}

	public boolean initialInit() {
		return this.initialInit;
	}

	@Override
	public int compareTo(IRequestItems other) {
		return Integer.compare(getID(), other.getID());
	}

	@Override
	public int getID() {
		return getRouter().getSimpleID();
	}

	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {}

	public boolean hasGenericInterests() {
		return false;
	}

	@Nullable
	public ISecurityProvider getSecurityProvider() {
		return SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
	}

	public boolean canBeDestroyedByPlayer(Player entityPlayer) {
		LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
		return station == null || station.getSecuritySettingsForPlayer(entityPlayer, true).removePipes;
	}

	@Override
	public boolean canBeDestroyed() {
		ISecurityProvider sec = getSecurityProvider();
		return sec == null || sec.canAutomatedDestroy();
	}

	public void setDestroyByPlayer() {
		destroyByPlayer = true;
	}

	@Override
	public boolean destroyByPlayer() {
		return destroyByPlayer;
	}

	@Override
	public boolean preventRemove() {
		return preventRemove;
	}

	@CCSecurtiyCheck
	public void checkCCAccess() throws PermissionException {
		ISecurityProvider sec = getSecurityProvider();
		if (sec != null) {
			int id = -1;
			if (container != null) {
				id = container.getLastCCID();
			}
			if (!sec.getAllowCC(id)) {
				throw new PermissionException();
			}
		}
	}

	public void queueUnroutedItemInformation(ItemIdentifierStack item, ItemRoutingInformation information) {
		if (item != null) {
			synchronized (queuedDataForUnroutedItems) {
				Queue<Pair<Integer, ItemRoutingInformation>> queue = queuedDataForUnroutedItems.computeIfAbsent(item.getItem(), k -> new LinkedList<>());
				queue.add(new Pair<>(item.getStackSize(), information));
			}
		}
	}

    @Nullable
	public ItemRoutingInformation getQueuedForItemStack(ItemIdentifierStack item) {
		synchronized (queuedDataForUnroutedItems) {
			Queue<Pair<Integer, ItemRoutingInformation>> queue = queuedDataForUnroutedItems.get(item.getItem());
			if (queue == null || queue.isEmpty()) {
				return null;
			}

			Pair<Integer, ItemRoutingInformation> pair = queue.peek();
			int wantItem = pair.getValue1();

			if (wantItem <= item.getStackSize()) {
				if (queue.remove() != pair) {
					LogisticsPipes.LOG.error("Item queue mismatch");
					return null;
				}
				if (queue.isEmpty()) {
					queuedDataForUnroutedItems.remove(item.getItem());
				}
				item.setStackSize(wantItem);
				return pair.getValue2();
			}
		}
		return null;
	}

	/**
	 * used as a distance offset when deciding which pipe to use NOTE: called
	 * very regularly, returning a pre-calculated int is probably appropriate.
	 */
	public double getLoadFactor() {
		return 0.0;
	}

	public void notifyOfItemArival(ItemRoutingInformation information) {
		inTransitToMe.remove(information);
		if (this instanceof IRequireReliableTransport) {
			((IRequireReliableTransport) this).itemArrived(information.getItem(), information.targetInfo);
		}
		if (this instanceof IRequireReliableFluidTransport) {
			ItemIdentifierStack stack = information.getItem();
			if (stack.getItem().isFluidContainer()) {
				FluidIdentifierStack liquid = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(stack, getWorld().registryAccess());
				if (liquid != null) {
					((IRequireReliableFluidTransport) this).liquidArrived(liquid.getFluid(), liquid.getAmount());
				}
			}
		}
	}

	@Override
	public int countOnRoute(ItemIdentifier it) {
		int count = 0;
		for (ItemRoutingInformation next : inTransitToMe) {
			if (next.getItem().getItem().equals(it)) {
				count += next.getItem().getStackSize();
			}
		}
		return count;
	}

	@Override
	public final int getIconIndex(@Nullable Direction connection) {
		TextureType texture = getTextureType(connection);
		if (textureBufferPowered) {
			return texture.powered;
		} else if (LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean()) {
			return texture.normal;
		} else {
			return texture.unpowered;
		}
	}

	public void addCrashReport(CrashReportCategory crashReportCategory) {
		addRouterCrashReport(crashReportCategory);
		crashReportCategory.setDetail("stillNeedReplace", stillNeedReplace);
	}

	protected void addRouterCrashReport(CrashReportCategory crashReportCategory) {
		crashReportCategory.setDetail("Router", getRouter().toString());
	}

	/* --- CCCommands --- */
	@CCCommand(description = "Returns the Router UUID as an integer; all pipes have a unique ID (runtime stable)")
	public int getRouterId() {
		return getRouter().getSimpleID();
	}

	@CCCommand(description = "Returns the Router UUID; all pipes have a unique ID (lifetime stable)")
	public String getRouterUUID() {
		return getRouter().getId().toString();
	}

	@CCCommand(description = "Returns the Router UUID for the givvin router Id")
	public String getRouterUUID(Double id) {
		IRouter router = SimpleServiceLocator.routerManager.getRouter(id.intValue());
		if (router == null) {
			return null;
		}
		return router.getId().toString();
	}

	@CCCommand(description = "Returns the TurtleConnect targeted for this Turtle on this LogisticsPipe")
	@CCDirectCall
	public boolean getTurtleConnect() {
		if (container != null) {
			return container.getTurtleConnect();
		}
		return false;
	}

	@CCCommand(description = "Sets the TurtleConnect targeted for this Turtle on this LogisticsPipe")
	@CCDirectCall
	public void setTurtleConnect(Boolean flag) {
		if (container != null) {
			container.setTurtleConnect(flag);
		}
	}

	@CCCommand(description = "Returns true if the computer is allowed to interact with the connected pipe.", needPermission = false)
	public boolean canAccess() {
		ISecurityProvider sec = getSecurityProvider();
		if (sec != null) {
			int id = -1;
			if (container != null) {
				id = container.getLastCCID();
			}
			return sec.getAllowCC(id);
		}
		return true;
	}

	@CCCommand(description = "Sends a message to the given computerId over the LP network. Event: " + CCConstants.LP_CC_MESSAGE_EVENT)
	@CCDirectCall
	public void sendMessage(final Double computerId, final Object message) {
		int sourceId = -1;
		if (container != null) {
			sourceId = container.getLastCCID(); // always 0 — ComputerCraft not available on 1.20.1
		}
		final int fSourceId = sourceId;
		BitSet set = new BitSet(ServerRouter.getBiggestSimpleID());
		getRouter().getIRoutersByCost().stream()
				.filter(exit -> !set.get(exit.destination.getSimpleID()))
				.forEach(exit -> {
					exit.destination.queueTask(10, (pipe, router1) -> pipe.handleMesssage(computerId.intValue(), message, fSourceId));
					set.set(exit.destination.getSimpleID());
				});
	}

	@CCCommand(description = "Sends a broadcast message to all Computer connected to this LP network. Event: " + CCConstants.LP_CC_BROADCAST_EVENT)
	@CCDirectCall
	public void sendBroadcast(final String message) {
		int sourceId = -1;
		if (container != null) {
			sourceId = container.getLastCCID(); // always 0 — ComputerCraft not available on 1.20.1
		}
		final int fSourceId = sourceId;
		BitSet set = new BitSet(ServerRouter.getBiggestSimpleID());
		getRouter().getIRoutersByCost().stream()
				.filter(exit -> !set.get(exit.destination.getSimpleID()))
				.forEach(exit -> {
					exit.destination.queueTask(10, (pipe, router1) -> pipe.handleBroadcast(message, fSourceId));
					set.set(exit.destination.getSimpleID());
				});
	}

	@CCCommand(description = "Returns the access to the pipe of the given router UUID")
	@ModDependentMethod(modId = LPConstants.computerCraftModID)
	@CCDirectCall
	public Object getPipeForUUID(String sUuid) throws PermissionException {
		if (!getUpgradeManager().hasCCRemoteControlUpgrade()) {
			throw new PermissionException();
		}
		UUID uuid = UUID.fromString(sUuid);
		int id = SimpleServiceLocator.routerManager.getIDforUUID(uuid);
		IRouter router = SimpleServiceLocator.routerManager.getRouter(id);
		if (router == null) {
			return null;
		}
		return router.getPipe();
	}

	@CCCommand(description = "Returns the global LP object which is used to access general LP methods.", needPermission = false)
	@CCDirectCall
	public Object getLP() throws PermissionException {
		return null;//LogisticsPipes.getComputerLP();
	}

	@CCCommand(description = "Returns true if the pipe has an internal module")
	public boolean hasLogisticsModule() {
		return getLogisticsModule() != null;
	}

	private void handleMesssage(int computerId, Object message, int sourceId) {
		if (container != null) {
			container.handleMesssage(computerId, message, sourceId);
		}
	}

	private void handleBroadcast(String message, int sourceId) {
		queueEvent(CCConstants.LP_CC_BROADCAST_EVENT, new Object[] { sourceId, message });
	}

	public void onWrenchClicked(Player entityplayer) {
		//do nothing, every pipe with a GUI should either have a LogisticsGuiModule or override this method
	}

	public void handleRFPowerArrival(double toSend) {
		powerHandler.addRFPower(toSend);
	}

	public void handleIC2PowerArrival(double toSend) {
		powerHandler.addIC2Power(toSend);
	}

	/* ISendRoutedItem */

	@Override
	public IRoutedItem sendStack(ItemStack stack, Pair<Integer, SinkReply> reply, ItemSendMode mode, Direction direction) {
		IRoutedItem itemToSend = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
		itemToSend.setDestination(reply.getValue1());
		if (reply.getValue2().isPassive) {
			if (reply.getValue2().isDefault) {
				itemToSend.setTransportMode(TransportMode.Default);
			} else {
				itemToSend.setTransportMode(TransportMode.Passive);
			}
		}
		itemToSend.setAdditionalTargetInformation(reply.getValue2().addInfo);
		queueRoutedItem(itemToSend, direction, mode);
		return itemToSend;
	}

	@Override
	public IRoutedItem sendStack(ItemStack stack, int destination, ItemSendMode mode, IAdditionalTargetInformation info, Direction direction) {
		IRoutedItem itemToSend = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
		itemToSend.setDestination(destination);
		itemToSend.setTransportMode(TransportMode.Active);
		itemToSend.setAdditionalTargetInformation(info);
		queueRoutedItem(itemToSend, direction, mode);
		return itemToSend;
	}

	@Override
	public LogisticsItemOrderManager getItemOrderManager() {
		orderItemManager = orderItemManager != null ? orderItemManager : new LogisticsItemOrderManager(this);
		return orderItemManager;
	}

	public LogisticsOrderManager<?, ?> getOrderManager() {
		return getItemOrderManager();
	}

	public void addPipeSign(Direction dir, IPipeSign type, Player player) {
		if (dir.ordinal() < 6) {
			if (signItem[dir.ordinal()] == null) {
				signItem[dir.ordinal()] = type;
				signItem[dir.ordinal()].init(this, dir);
			}
			if (container != null) {
				sendSignData(player, true);
				refreshRender(false);
			}
		}
	}

	public void sendSignData(Player player, boolean sendToAll) {
		List<Integer> types = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] == null) {
				types.add(-1);
			} else {
				List<Class<? extends IPipeSign>> typeClasses = ItemPipeSignCreator.signTypes;
				for (int j = 0; j < typeClasses.size(); j++) {
					if (typeClasses.get(j) == signItem[i].getClass()) {
						types.add(j);
						break;
					}
				}
			}
		}
		final PipeSignTypesMessage message = new PipeSignTypesMessage(getPos(), types);
		if (sendToAll) {
			TargetLookup.sendToChunkWatchers(container, message);
		} else if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, message);
		}
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				final CustomPacketPayload signPayload = signItem[i].getPacket();
				if (signPayload != null) {
					TargetLookup.sendToChunkWatchers(container, signPayload);
					if (player instanceof ServerPlayer serverPlayer) {
						PacketDistributor.sendToPlayer(serverPlayer, signPayload);
					}
				}
			}
		}
	}

	public void removePipeSign(Direction dir, Player player) {
		if (dir.ordinal() < 6) {
			signItem[dir.ordinal()] = null;
		}
		sendSignData(player, true);
		refreshRender(false);
	}

	public boolean hasPipeSign(Direction dir) {
		if (dir.ordinal() < 6) {
			return signItem[dir.ordinal()] != null;
		}
		return false;
	}

	public void activatePipeSign(Direction dir, Player player) {
		if (dir.ordinal() < 6) {
			if (signItem[dir.ordinal()] != null) {
				signItem[dir.ordinal()].activate(player);
			}
		}
	}

	public List<Pair<Direction, IPipeSign>> getPipeSigns() {
		List<Pair<Direction, IPipeSign>> list = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				list.add(new Pair<>(DirectionUtil.getOrientation(i), signItem[i]));
			}
		}
		return list;
	}

	public void handleSignPacket(List<Integer> types) {
		if (!MainProxy.isClient(getWorld())) {
			return;
		}
		for (int i = 0; i < 6; i++) {
			// A message that named fewer than six sides used to walk off the end of the list.
			int integer = i < types.size() ? types.get(i) : -1;
			if (integer >= 0) {
				Class<? extends IPipeSign> type = ItemPipeSignCreator.signTypes.get(integer);
				if (signItem[i] == null || signItem[i].getClass() != type) {
					try {
						signItem[i] = type.newInstance();
						signItem[i].init(this, DirectionUtil.getOrientation(i));
					} catch (InstantiationException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				signItem[i] = null;
			}
		}
	}

	@Nullable
	public IPipeSign getPipeSign(@Nullable Direction dir) {
		if (dir == null) return null;
		return signItem[dir.ordinal()];
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeBoolean(isOpaque());
	}

	@Override
	public void readData(LPDataInput input) {
		isOpaqueClientSide = input.readBoolean();
	}

	@Override
	public boolean isOpaque() {
		if (MainProxy.isClient(getWorld())) {
			return LPConfigs.COMMON.OPAQUE.getAsBoolean() || isOpaqueClientSide;
		} else {
			return LPConfigs.COMMON.OPAQUE.getAsBoolean() || this.getUpgradeManager().isOpaque();
		}
	}

	@Override
	public void addStatusInformation(List<StatusEntry> status) {
		status.add(StatusEntry.of("Send Queue", sendQueue));
		status.add(StatusEntry.of("In Transit To Me", inTransitToMe));
	}

	@Override
	public int getSourceID() {
		return getRouterId();
	}

	@Override
    public DebugLogController getDebug() {
		return debug;
	}

	@Override
	public void setPreventRemove(boolean flag) {
		preventRemove = flag;
	}

	@Override
	public boolean isRoutedPipe() {
		return true;
	}

	@Override
	public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double traveled, double max,
			List<DoubleCoordinates> visited) {
		if (!stillNeedReplace) {
			if (getRouterId() == destinationint) {
				return 0;
			}
			ExitRoute route = getRouter().getExitFor(destinationint, isActive, ident);
			if (route != null && route.exitOrientation != ignore) {
				if (route.distanceToDestination + traveled >= max) {
					return Integer.MAX_VALUE;
				}
				return route.distanceToDestination;
			}
		}
		return Integer.MAX_VALUE;
	}

	protected void triggerConnectionCheck() {
		recheckConnections = true;
	}

	@Override
	public CacheHolder getCacheHolder() {
		if (cacheHolder == null) {
			if (container instanceof ILPTEInformation containerInfo && containerInfo.getLPTileEntityObject() != null) {
				cacheHolder = containerInfo.getLPTileEntityObject().getCacheHolder();
			} else {
				cacheHolder = new CacheHolder();
			}
		}
		return cacheHolder;
	}

	public enum ItemSendMode {
		Normal,
		Fast
	}

	@Override
	public void finishInit() {
		super.finishInit();
		if (isInitialized()) {
			MainProxy.runOnServer(getWorld(), () -> () -> {
				if (this instanceof PropertyHolder) {
					UtilKt.addObserver(((PropertyHolder) this).getProperties(), (prop) -> {
						markTileDirty();
						return Unit.INSTANCE;
					});
				}
			});

			if (getLogisticsModule() != null) {
				getLogisticsModule().finishInit();
			}
		}
	}
}
