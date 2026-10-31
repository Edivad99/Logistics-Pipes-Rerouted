package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.LogisticsPipes;
import logisticspipes.Translations;
import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.property.PropertyHolder;
import logisticspipes.api.property.PropertyUtil;
import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.client.renderer.hud.LogisticsHUDRenderer;
import logisticspipes.connection.AdjacentFactory;
import logisticspipes.connection.NoAdjacent;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.ILevelProvider;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWatchingHandler;
import logisticspipes.interfaces.routing.FilteredRoute;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PipeSignTypesMessage;
import logisticspipes.network.to_client.pipe.PipeStatsMessage;
import logisticspipes.network.to_server.pipe.RequestPipeSignsMessage;
import logisticspipes.network.to_server.pipe.RequestRoutingLasersMessage;
import logisticspipes.particle.Particles;
import logisticspipes.particle.PipeFXRenderHandler;
import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.PipeSignType;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.order.ItemOrderManager;
import logisticspipes.request.order.OrderManager;
import logisticspipes.routing.ClientRouterManager;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.item.DeliveryHandler;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.routing.item.ItemRoutingInformation;
import logisticspipes.routing.item.PipeDeliveryHandler;
import logisticspipes.routing.item.RouteResolver;
import logisticspipes.routing.sink.SinkReply;
import logisticspipes.security.SecurityPermission;
import logisticspipes.security.SecurityPermissions;
import logisticspipes.textures.PipeTexture;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.util.PipeConfigTools;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.inventory.OrdererMenu;
import logisticspipes.world.inventory.PipeControllerMenu;
import logisticspipes.world.item.ItemPipeController;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.world.level.block.entity.LogisticsSecurityBlockEntity;

public abstract class CoreRoutedPipe extends CoreUnroutedPipe
    implements IRequestItems, ILevelProvider, IWatchingHandler, IPipeServiceProvider,
    ILPPositionProvider {

    private static int PIPE_COUNT = 0;
    private static final String SEND_QUEUE_KEY = "sendqueue";
    public final PlayerCollectionList watchers = new PlayerCollectionList();
    protected final PriorityBlockingQueue<ItemRoutingInformation> inTransitToMe =
        new PriorityBlockingQueue<>(10, new ItemRoutingInformation.DelayComparator());
    protected final LinkedList<QueuedItem> sendQueue = new LinkedList<>();
    protected final Map<ItemIdentifier, Queue<UnroutedItemInfo>> queuedDataForUnroutedItems =
        Collections.synchronizedMap(new TreeMap<>());
    protected final Object routerIdLock = new Object();
    final protected UpgradeManager upgradeManager = new UpgradeManager(this);
    private final PowerSupplierHandler powerHandler = new PowerSupplierHandler(this);
    @Getter
    private final List<IOrderInfoProvider> clientSideOrderManager = new ArrayList<>();
    private final int[] queuedParticles = new int[Particles.values().length];
    public boolean textureBufferPowered;
    public long delayTo = 0;
    public int repeatFor = 0;
    public int serverRoutingTableSize = 0;
    protected boolean stillNeedReplace = true;
    @Nullable
    protected IRouter router;
    @Nullable
    protected String routerId;
    protected int delayOffset;
    protected boolean initialInit = true;
    @Nullable
    protected RouteResolver routeResolver;
    @Nullable
    protected DeliveryHandler deliveryHandler;
    @Nullable
    protected ItemOrderManager orderItemManager = null;
    protected int throttleTime = 20;
    protected @Nullable IPipeSign[] signItem = new IPipeSign[6];
    private TrafficCounts sessionCounts = TrafficCounts.ZERO;
    private TrafficCounts lifetimeCounts = TrafficCounts.ZERO;
    private boolean recheckConnections = false;
    @Setter
    @Getter
    private boolean enabled = true;
    private boolean preventRemove = false;
    private boolean destroyByPlayer = false;
    private int throttleTimeLeft;
    private boolean hasQueuedParticles = false;
    private boolean isOpaqueClientSide = false;

    /**
     * Caches adjacent state, only on server side
     */
    @Getter(AccessLevel.PROTECTED)
    private Adjacent adjacent = NoAdjacent.INSTANCE;
    @Nullable
    private CacheHolder cacheHolder;

    public CoreRoutedPipe(Item item) {
        this(new PipeTransportLogistics(true), item);
    }

    public CoreRoutedPipe(PipeTransportLogistics transport, Item item) {
        super(transport, item);
        CoreRoutedPipe.PIPE_COUNT++;
    }

    /**
     * Returns all adjacent on a regular routed pipe.
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
        adjacent = AdjacentFactory.createAdjacentCache(this);
    }

    @Override
    public void initialize() {
        super.initialize();
        throttleTimeLeft = 20 + new Random().nextInt(LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt());
        //Roughly spread pipe updates throughout the frequency, no need to maintain balance
        delayOffset = CoreRoutedPipe.PIPE_COUNT % LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt();
    }

    @Override
    public void markTileDirty() {
        if (getContainer() != null) {
            getContainer().setChanged();
        }
    }

    public RouteResolver getRouteResolver() {
        if (routeResolver == null) {
            routeResolver = new RouteResolver(getRouter(), getDeliveryHandler(), this);
        }
        return routeResolver;
    }

    public DeliveryHandler getDeliveryHandler() {
        if (deliveryHandler == null) {
            deliveryHandler = new PipeDeliveryHandler(this, getRouter());
        }
        return deliveryHandler;
    }

    @Override
    public ISlotUpgradeManager getUpgradeManager(@Nullable ModulePositionType slot, int positionInt) {
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
        sendQueue.addLast(new QueuedItem(routedItem, from, ItemSendMode.NORMAL));
        sendQueueChanged(false);
    }

    public void queueRoutedItem(IRoutedItem routedItem, Direction from, ItemSendMode mode) {
        Objects.requireNonNull(from);
        sendQueue.addLast(new QueuedItem(routedItem, from, mode));
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

        IRouter r = RouterManager.get().getServerRouter(routedItem.getDestination());
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
        sessionCounts = sessionCounts.plusSent(1);
        lifetimeCounts = lifetimeCounts.plusSent(1);
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
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        for (BlockPos pos : myPositions) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ChestBlock)) {
                continue;
            }
            if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                continue;
            }
            BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
            if (otherPositions.contains(partner)) {
                return true;
            }
        }
        return false;
    }

    /***
     * first tick just create a router and do nothing.
     */
    public void firstInitialiseTick() {
        getRouter();
        if (getLevel().isClientSide()) {
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
    public void ignoreDisableUpdateEntity() {
    }

    @Override
    public final void updateEntity() {
        debug.tick();
        spawnParticleTick();
        if (stillNeedReplace) {
            stillNeedReplace = false;
            //first tick just create a router and do nothing.
            firstInitialiseTick();
            return;
        }
        if (repeatFor > 0) {
            if (delayTo < System.currentTimeMillis()) {
                delayTo = System.currentTimeMillis() + 200;
                repeatFor--;
                getLevel().updateNeighborsAt(getPos(), getLevel().getBlockState(getPos()).getBlock());
            }
        }

        // remove old items _inTransit -- these should have arrived, but have probably been lost instead.
        // In either case, it will allow a re-send so that another attempt to re-fill the inventory can be made.
        while (inTransitToMe.peek() != null && inTransitToMe.peek().getTickToTimeOut() <= 0) {
            final ItemRoutingInformation polledInfo = inTransitToMe.poll();
            if (polledInfo != null) {
                if (LogisticsPipes.isDEBUG()) {
                    LogisticsPipes.LOG.info(
                        "Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
                }
                debug.log("Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
            }
        }
        //update router before ticking logic/transport
        final boolean doFullRefresh =
            getLevel().getGameTime() % LPConfigs.COMMON.LOGISTICS_DETECTION_FREQUENCY.getAsInt() == delayOffset
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
                case NORMAL -> {
                    QueuedItem itemToSend = sendQueue.getFirst();
                    sendRoutedItem(itemToSend.item(), itemToSend.from());
                    sendQueue.removeFirst();
                    for (int i = 0; i < 16 &&
                        !sendQueue.isEmpty() &&
                        sendQueue.getFirst().mode() == ItemSendMode.FAST; i++) {
                        itemToSend = sendQueue.getFirst();
                        sendRoutedItem(itemToSend.item(), itemToSend.from());
                        sendQueue.removeFirst();
                    }
                    sendQueueChanged(false);
                }
                case FAST -> {
                    for (int i = 0; i < 16; i++) {
                        if (!sendQueue.isEmpty()) {
                            QueuedItem itemToSend = sendQueue.getFirst();
                            sendRoutedItem(itemToSend.item(), itemToSend.from());
                            sendQueue.removeFirst();
                        }
                    }
                    sendQueueChanged(false);
                }
            }
        }
        if (getLevel().isClientSide()) {
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

    protected void onAllowedRemoval() {
    }

    public void throttledUpdateEntity() {
    }

    protected void delayThrottle() {
        //delay 6(+1) ticks to prevent suppliers from ticking between a item arriving at them and the item hitting their adj. inv
        if (throttleTimeLeft < 7) {
            throttleTimeLeft = 7;
        }
    }

    @Override
    public boolean isNthTick(int n) {
        return ((getLevel().getGameTime() + delayOffset) % n == 0);
    }

    private void doDebugStuff(Player entityplayer) {
        //entityplayer.level().setWorldTime(4951);
        if (entityplayer.level().isClientSide()) {
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
        List<@Nullable List<ExitRoute>> table = router.getRouteTable();
        for (int i = 0; i < table.size(); i++) {
            // One read of the entry: the routing table is replaced wholesale by another thread, so
            // repeated get(i) calls can disagree with the null check that guards them.
            final List<ExitRoute> routes = table.get(i);
            if (routes != null) {
                if (!routes.isEmpty()) {
                    sb.append(i).append(" -> ").append(routes.getFirst().destination.getSimpleID()).append('\n');
                    for (ExitRoute route : routes) {
                        sb.append("\t\t via ").append(route.exitOrientation).append("(")
                            .append(route.distanceToDestination).append(" distance)").append('\n');
                    }
                }
            }
        }
        sb.append('\n');
        sb.append("++++++++++CONNECTIONS+++++++++++++++\n");
        sb.append(Arrays.toString(Direction.values())).append('\n');
        sb.append(Arrays.toString(router.sideDisconnected)).append('\n');
        if (getContainer() != null) {
            sb.append(Arrays.toString(getContainer().pipeConnectionsBuffer)).append('\n');
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

    @Override
    public final void onBlockRemoval() {
        try {
            onAllowedRemoval();
            super.onBlockRemoval();
            //Just in case
            CoreRoutedPipe.PIPE_COUNT = Math.max(CoreRoutedPipe.PIPE_COUNT - 1, 0);

            transport.dropBuffer();
            getOriginalUpgradeManager().dropUpgrades();
        } catch (Exception e) {
            LogisticsPipes.LOG.error("Exception during pipe teardown at ({})", getPos(), e);
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
        return getCenterTexture().ordinal();
    }

    public abstract PipeTexture getCenterTexture();

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
        if (getLevel() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < this.queuedParticles.length; i++) {
                if (this.queuedParticles[i] > 0) {
                    var amount = this.queuedParticles[i];
                    serverLevel.sendParticles(Particles.values()[i].getSparkleFXParticleOptions(amount),
                        getPos().getX(), getPos().getY(), getPos().getZ(), amount, 0, 0, 0, 1);
                }
            }
        } else if (getLevel() instanceof ClientLevel) {
            if (Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST) {
                for (int i = 0; i < queuedParticles.length; i++) {
                    if (this.queuedParticles[i] > 0) {
                        PipeFXRenderHandler.spawnGenericParticle(Particles.values()[i],
                            getPos().getX(), getPos().getY(), getPos().getZ(), queuedParticles[i]);
                    }
                }
            }
        }
        Arrays.fill(queuedParticles, 0);
        hasQueuedParticles = false;
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
        output.putLong("stat_lifetime_sent", lifetimeCounts.sent());
        output.putLong("stat_lifetime_received", lifetimeCounts.received());
        output.putLong("stat_lifetime_relayed", lifetimeCounts.relayed());
        if (getLogisticsModule() != null) {
            getLogisticsModule().serialize(output);
        }
        output.putChild("upgradeManager", upgradeManager);
        output.putChild("powerHandler", powerHandler);

        ValueOutput.ValueOutputList sendqueue = output.childrenList(SEND_QUEUE_KEY);
        for (QueuedItem p : sendQueue) {
            ValueOutput tagentry = sendqueue.addChild();
            p.item().serialize(tagentry.child("entityitem"));
            tagentry.putByte("from", (byte) (p.from().ordinal()));
            tagentry.putByte("mode", (byte) (p.mode().ordinal()));
        }

        for (int i = 0; i < 6; i++) {
            if (signItem[i] != null) {
                output.putBoolean("PipeSign_" + i, true);
                output.putInt("PipeSign_" + i + "_type", signItem[i].type().ordinal());
                output.putChild("PipeSign_" + i + "_tags", signItem[i]);
            } else {
                output.putBoolean("PipeSign_" + i, false);
            }
        }

        if (this instanceof PropertyHolder) {
            PropertyHolder.serialize(output, (PropertyHolder) this);
        }
    }

    /**
     * Drops the send queue from a copy of the saved data meant for a client: its items are server
     * items, and the client only sees them once they are in a pipe.
     */
    public static void removeServerOnlyData(CompoundTag tag) {
        tag.remove(SEND_QUEUE_KEY);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);

        synchronized (routerIdLock) {
            routerId = input.getStringOr("routerId", "");
        }

        lifetimeCounts = new TrafficCounts(
            input.getLongOr("stat_lifetime_sent", 0L),
            input.getLongOr("stat_lifetime_received", 0L),
            input.getLongOr("stat_lifetime_relayed", 0L));
        if (getLogisticsModule() != null) {
            getLogisticsModule().deserialize(input);
        }
        upgradeManager.deserialize(input.childOrEmpty("upgradeManager"));
        powerHandler.deserialize(input.childOrEmpty("powerHandler"));

        sendQueue.clear();
        for (ValueInput tagentry : input.childrenListOrEmpty(SEND_QUEUE_KEY)) {
            LPTravelingItemServer item = new LPTravelingItemServer(tagentry.childOrEmpty("entityitem"));
            Direction from = Direction.values()[tagentry.getByteOr("from", (byte) 0)];
            ItemSendMode mode = ItemSendMode.values()[tagentry.getByteOr("mode", (byte) 0)];
            sendQueue.add(new QueuedItem(item, from, mode));
        }
        for (int i = 0; i < 6; i++) {
            if (input.getBooleanOr("PipeSign_" + i, false)) {
                PipeSignType type = PipeSignType.byId(input.getIntOr("PipeSign_" + i + "_type", 0));
                if (type != null && type.isAllowedFor(this)) {
                    signItem[i] = type.create(this, Direction.from3DDataValue(i));
                    signItem[i].deserialize(input.childOrEmpty("PipeSign_" + i + "_tags"));
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
            LogisticsPipes.LOG.debug("Pipe not ready at ({}, '{}')", this.getPos(),
                getLevel() != null ? getLevel().dimension().identifier().toString() : "unknown");
        }
        if (router == null) {
            synchronized (routerIdLock) {

                UUID routerIntId = null;
                if (routerId != null && !routerId.isEmpty()) {
                    routerIntId = UUID.fromString(routerId);
                }
                router = getLevel().isClientSide()
                    ? ClientRouterManager.get().getOrCreateRouter(routerIntId, getLevel(), getPos())
                    : RouterManager.get().getOrCreateRouter(routerIntId, getLevel(), getPos());
            }
        }
        return router;
    }

    public abstract @Nullable LogisticsModule getLogisticsModule();

    @Override
    public final boolean blockActivated(Player player) {
        if (getContainer() == null) {
            return super.blockActivated(player);
        }
        SecurityPermissions permissions = null;
        if (!player.level().isClientSide()) {
            LogisticsSecurityBlockEntity station = RouterManager.get().getStation(
                getOriginalUpgradeManager().getSecurityID());
            if (station != null) {
                permissions = station.getPermissionsForPlayer(player, true);
            }
        }

        if (ItemPipeController.isHeldBy(player)) {
            if (!player.level().isClientSide()) {
                if (permissions == null || permissions.allows(SecurityPermission.OPEN_NETWORK_MONITOR)) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(new SimpleMenuProvider(
                                (containerId, inventory, viewer) ->
                                    new PipeControllerMenu(containerId, inventory, this),
                                Component.empty()),
                            buffer -> buffer.writeBlockPos(getPos()));
                    }
                } else {
                    player.sendOverlayMessage(Component.translatable(Translations.Chat.PERMISSION_DENIED));
                }
            }
            return true;
        }

        if (handleClick(player, permissions)) {
            return true;
        }

        ItemStack itemStack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (itemStack.isEmpty()) {
            if (!player.isCrouching()) {
                return false;
            }
            if (player.level().isClientSide()) {
                if (LogisticsHUDRenderer.instance().hasLasers()) {
                    LogisticsHUDRenderer.instance().resetLasers();
                } else {
                    ClientPacketDistributor.sendToServer(new RequestRoutingLasersMessage(getPos()));
                }
            }
            if (LogisticsPipes.isDEBUG()) {
                doDebugStuff(player);
            }
            return true;
        }

        if (itemStack.is(LPItems.REMOTE_ORDERER)) {
            if (!player.level().isClientSide()) {
                if (permissions == null || permissions.allows(SecurityPermission.OPEN_REQUEST)) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        OrdererMenu.open(serverPlayer, this);
                    }
                } else {
                    player.sendOverlayMessage(Component.translatable(Translations.Chat.PERMISSION_DENIED));
                }
            }
            return true;
        }

        if (PipeConfigTools.canConfigure(itemStack)) {
            if (!player.level().isClientSide()) {
                if (permissions == null || permissions.allows(SecurityPermission.OPEN_GUI)) {
                    final LogisticsModule module = getLogisticsModule();
                    if (module instanceof IModuleMenuProvider && player instanceof ServerPlayer serverPlayer) {
                        IModuleMenuProvider.open(serverPlayer, module);
                    } else {
                        onWrenchClicked(player);
                    }
                } else {
                    player.sendOverlayMessage(Component.translatable(Translations.Chat.PERMISSION_DENIED));
                }
            }
            return true;
        }

        if (!(player.isCrouching()) && getOriginalUpgradeManager().tryIserting(getLevel(), player)) {
            return true;
        }

        return super.blockActivated(player);
    }

    protected boolean handleClick(Player entityplayer, @Nullable SecurityPermissions permissions) {
        return false;
    }

    public void refreshRender(boolean spawnPart) {
        getContainer().scheduleRenderUpdate();
        if (spawnPart) {
            spawnParticle(Particles.GREEN_SPARKLE, 3);
        }
    }

    public void refreshConnectionAndRender(boolean spawnPart) {
        getContainer().scheduleNeighborChange();
        if (spawnPart) {
            spawnParticle(Particles.GREEN_SPARKLE, 3);
        }
    }

    public void receivedItem(int count) {
        sessionCounts = sessionCounts.plusReceived(count);
        lifetimeCounts = lifetimeCounts.plusReceived(count);
        updateStats();
    }

    public void relayedItem(int count) {
        sessionCounts = sessionCounts.plusRelayed(count);
        lifetimeCounts = lifetimeCounts.plusRelayed(count);
        updateStats();
    }

    public TrafficCounts sessionCounts() {
        return sessionCounts;
    }

    public TrafficCounts lifetimeCounts() {
        return lifetimeCounts;
    }

    /**
     * How many other pipes this one can reach.
     */
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
        sessionCounts = session;
        lifetimeCounts = lifetime;
        serverRoutingTableSize = routingTableSize;
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
    public void itemCouldNotBeSend(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        if (this instanceof IRequireReliableTransport) {
            ((IRequireReliableTransport) this).itemLost(item, info);
        }
    }

    public boolean isLockedExit(Direction orientation) {
        return false;
    }

    public boolean logisticsIsPipeConnected(BlockEntity blockEntity, Direction dir) {
        return false;
    }

    @Override
    public final boolean canPipeConnect(BlockEntity tile, Direction dir) {
        return canPipeConnect(tile, dir, false);
    }

    @Override
    public final boolean canPipeConnect(BlockEntity tile, Direction dir, boolean ignoreSystemDisconnection) {
        Direction side = OrientationsUtil.getOrientationOfTilewithTile(getContainer(), tile);
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
        if (getContainer() != null && !stillNeedReplace) {
            if (getLevel().isClientSide()) {
                throw new IllegalStateException("Wont do connectionUpdate on client-side");
            }
            getContainer().scheduleNeighborChange();
            BlockState state = getLevel().getBlockState(getPos());
            getLevel().updateNeighborsAt(getPos(), state.getBlock());
        }
    }

    @Nullable
    public UUID getSecurityID() {
        return getOriginalUpgradeManager().getSecurityID();
    }

    public void insetSecurityID(UUID id) {
        getOriginalUpgradeManager().insetSecurityID(id);
    }

    @Nullable
    public List<FilteredRoute<ILogisticsPowerProvider>> getRoutedPowerProviders() {
        if (getLevel().isClientSide()) {
            return null;
        }
        if (stillNeedReplace) {
            return null;
        }
        return getRouter().getPowerProvider();
    }

    @Override
    public boolean useEnergy(int amount) {
        return useEnergy(amount, null, true);
    }

    /* Power System */

    public boolean useEnergy(int amount, boolean sparkles) {
        return useEnergy(amount, null, sparkles);
    }

    @Override
    public boolean canUseEnergy(int amount) {
        return canUseEnergy(amount, null);
    }

    @Override
    public boolean canUseEnergy(int amount, @Nullable List<Object> providersToIgnore) {
        if (getLevel().isClientSide()) {
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
        List<FilteredRoute<ILogisticsPowerProvider>> list = getRoutedPowerProviders();
        if (list == null) {
            return false;
        }
        outer:
        for (FilteredRoute<ILogisticsPowerProvider> provider : list) {
            for (IFilter filter : provider.filters()) {
                if (filter.blockPower()) {
                    continue outer;
                }
            }
            if (provider.target().canUseEnergy(amount, providersToIgnore)) {
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
        if (getLevel().isClientSide()) {
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
        List<FilteredRoute<ILogisticsPowerProvider>> list = getRoutedPowerProviders();
        if (list == null) {
            return false;
        }
        outer:
        for (FilteredRoute<ILogisticsPowerProvider> provider : list) {
            for (IFilter filter : provider.filters()) {
                if (filter.blockPower()) {
                    continue outer;
                }
            }
            if (provider.target().canUseEnergy(amount, providersToIgnore)) {
                if (provider.target().useEnergy(amount, providersToIgnore)) {
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

    public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
    }

    public boolean hasGenericInterests() {
        return false;
    }

    @Nullable
    public ISecurityProvider getSecurityProvider() {
        return RouterManager.get().getStation(getOriginalUpgradeManager().getSecurityID());
    }

    public boolean canBeDestroyedByPlayer(Player entityPlayer) {
        LogisticsSecurityBlockEntity station = RouterManager.get().getStation(
            getOriginalUpgradeManager().getSecurityID());
        return station == null || station.getPermissionsForPlayer(entityPlayer, true).allows(SecurityPermission.REMOVE_PIPES);
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

    public void queueUnroutedItemInformation(ItemIdentifierStack item, ItemRoutingInformation information) {
        synchronized (queuedDataForUnroutedItems) {
            Queue<UnroutedItemInfo> queue = queuedDataForUnroutedItems
                .computeIfAbsent(item.getItem(), _ -> new LinkedList<>());
            queue.add(new UnroutedItemInfo(item.getStackSize(), information));
        }
    }

    @Nullable
    public ItemRoutingInformation getQueuedForItemStack(ItemIdentifierStack item) {
        synchronized (queuedDataForUnroutedItems) {
            Queue<UnroutedItemInfo> queue = queuedDataForUnroutedItems.get(item.getItem());
            if (queue == null || queue.isEmpty()) {
                return null;
            }

            UnroutedItemInfo pair = queue.peek();
            int wantItem = pair.stackSize();

            if (wantItem <= item.getStackSize()) {
                if (queue.remove() != pair) {
                    LogisticsPipes.LOG.error("Item queue mismatch");
                    return null;
                }
                if (queue.isEmpty()) {
                    queuedDataForUnroutedItems.remove(item.getItem());
                }
                item.setStackSize(wantItem);
                return pair.information();
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

    public void notifyOfItemArrival(ItemRoutingInformation information) {
        inTransitToMe.remove(information);
        if (this instanceof IRequireReliableTransport requireReliableTransport) {
            requireReliableTransport.itemArrived(information.getItem(), information.targetInfo);
        }
        if (this instanceof IRequireReliableFluidTransport requireReliableFluidTransport) {
            ItemIdentifierStack stack = information.getItem();
            if (stack.getItem().isFluidContainer()) {
                FluidIdentifierStack liquid = LogisticsFluidContainer.getFluid(stack);
                if (liquid != null) {
                    requireReliableFluidTransport.liquidArrived(liquid.getFluid(), liquid.getAmount());
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

    public void addCrashReport(CrashReportCategory crashReportCategory) {
        addRouterCrashReport(crashReportCategory);
        crashReportCategory.setDetail("stillNeedReplace", stillNeedReplace);
    }

    protected void addRouterCrashReport(CrashReportCategory crashReportCategory) {
        crashReportCategory.setDetail("Router", getRouter().toString());
    }

    public int getRouterId() {
        return getRouter().getSimpleID();
    }

    public void onWrenchClicked(Player entityplayer) {
        //do nothing, every pipe with a GUI should either have a LogisticsGuiModule or override this method
    }

    public void handleFEPowerArrival(int toSend) {
        powerHandler.addFEPower(toSend);
    }

    @Override
    public IRoutedItem sendStack(ItemStack stack, int destination, SinkReply reply, ItemSendMode mode,
        Direction direction) {
        IRoutedItem itemToSend = new LPTravelingItemServer(stack);
        itemToSend.setDestination(destination);
        if (reply.isPassive) {
            if (reply.isDefault) {
                itemToSend.setTransportMode(TransportMode.Default);
            } else {
                itemToSend.setTransportMode(TransportMode.Passive);
            }
        }
        itemToSend.setAdditionalTargetInformation(reply.addInfo);
        queueRoutedItem(itemToSend, direction, mode);
        return itemToSend;
    }

    /* ISendRoutedItem */

    @Override
    public IRoutedItem sendStack(ItemStack stack, int destination, ItemSendMode mode,
        @Nullable IAdditionalTargetInformation info, Direction direction) {
        IRoutedItem itemToSend = new LPTravelingItemServer(stack);
        itemToSend.setDestination(destination);
        itemToSend.setTransportMode(TransportMode.Active);
        itemToSend.setAdditionalTargetInformation(info);
        queueRoutedItem(itemToSend, direction, mode);
        return itemToSend;
    }

    @Override
    public ItemOrderManager getItemOrderManager() {
        orderItemManager = orderItemManager != null ? orderItemManager : new ItemOrderManager(this);
        return orderItemManager;
    }

    public OrderManager<?, ?> getOrderManager() {
        return getItemOrderManager();
    }

    public void addPipeSign(Direction dir, IPipeSign sign, Player player) {
        if (dir.ordinal() < 6) {
            if (signItem[dir.ordinal()] == null) {
                signItem[dir.ordinal()] = sign;
            }
            if (getContainer() != null) {
                sendSignData(player, true);
                refreshRender(false);
            }
        }
    }

    public void sendSignData(Player player, boolean sendToAll) {
        List<Integer> types = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            types.add(signItem[i] == null ? -1 : signItem[i].type().ordinal());
        }
        final PipeSignTypesMessage message = new PipeSignTypesMessage(getPos(), types);
        if (sendToAll) {
            TargetLookup.sendToChunkWatchers(getContainer(), message);
        } else if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        }
        for (int i = 0; i < 6; i++) {
            if (signItem[i] != null) {
                final CustomPacketPayload signPayload = signItem[i].getPacket();
                if (signPayload != null) {
                    TargetLookup.sendToChunkWatchers(getContainer(), signPayload);
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

    public List<SidedSign> getPipeSigns() {
        List<SidedSign> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (signItem[i] != null) {
                // from3DDataValue rather than DirectionUtil.getOrientation, which answers null out of
                // range: the loop never leaves it, and a sign always has a side.
                list.add(new SidedSign(Direction.from3DDataValue(i), signItem[i]));
            }
        }
        return list;
    }

    public void handleSignPacket(List<Integer> types) {
        if (!getLevel().isClientSide()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            // A message that named fewer than six sides used to walk off the end of the list.
            PipeSignType type = PipeSignType.byId(i < types.size() ? types.get(i) : -1);
            if (type == null || !type.isAllowedFor(this)) {
                signItem[i] = null;
            } else if (signItem[i] == null || signItem[i].type() != type) {
                signItem[i] = type.create(this, Direction.from3DDataValue(i));
            }
        }
    }

    @Nullable
    public IPipeSign getPipeSign(@Nullable Direction dir) {
        if (dir == null) {
            return null;
        }
        return signItem[dir.ordinal()];
    }

    @Override
    public void writeState(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isOpaque());
    }

    @Override
    public void readState(FriendlyByteBuf buffer) {
        isOpaqueClientSide = buffer.readBoolean();
    }

    @Override
    public boolean isOpaque() {
        if (getLevel().isClientSide()) {
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
    public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive,
        double traveled, double max,
        List<BlockPos> visited) {
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

    public void triggerConnectionCheck() {
        recheckConnections = true;
    }

    @Override
    public CacheHolder getCacheHolder() {
        // The container's holder is the one the router clears when the network changes.
        if (getContainer() != null) {
            return getContainer().getCacheHolder();
        }
        if (cacheHolder == null) {
            cacheHolder = new CacheHolder();
        }
        return cacheHolder;
    }

    @Override
    public void finishInit() {
        super.finishInit();
        if (isInitialized()) {
            final Level level = getLevel();
            if (level != null && !level.isClientSide() && this instanceof PropertyHolder) {
                PropertyUtil.addObserver(((PropertyHolder) this).getProperties(), prop -> markTileDirty());
            }

            if (getLogisticsModule() != null) {
                getLogisticsModule().finishInit();
            }
        }
    }

    public enum ItemSendMode {
        NORMAL,
        FAST
    }

    /** An item waiting to leave the pipe: the side it came in from, and how fast it may go. */
    protected record QueuedItem(IRoutedItem item, Direction from, ItemSendMode mode) {}

    /** Where to route the next unrouted stack of an item, once at least {@code stackSize} of it arrives. */
    protected record UnroutedItemInfo(int stackSize, ItemRoutingInformation information) {}

    /** A sign on one side of the pipe. */
    public record SidedSign(Direction side, IPipeSign sign) {}

    /**
     * What is waiting to leave, one stack per kind of item, in the order each kind first comes up.
     */
    protected List<@Nullable ItemIdentifierStack> sendQueueStacks() {
        List<ItemIdentifierStack> stacks = new ArrayList<>();
        outer:
        for (QueuedItem queued : sendQueue) {
            ItemIdentifierStack queuedStack = queued.item().getItemIdentifierStack();
            for (ItemIdentifierStack stack : stacks) {
                if (stack.getItem().equals(queuedStack.getItem())) {
                    stack.setStackSize(stack.getStackSize() + queuedStack.getStackSize());
                    continue outer;
                }
            }
            stacks.add(new ItemIdentifierStack(queuedStack));
        }
        // The message's list allows empty slots, which the send queue never has.
        return new ArrayList<>(stacks);
    }

    /**
     * How many items went through a pipe, over one window of time.
     *
     * <p>The same three counters are kept twice: once since the server started, once for the
     * pipe's whole life.
     */
    public record TrafficCounts(long sent, long received, long relayed) {

        public static final TrafficCounts ZERO = new TrafficCounts(0, 0, 0);

        public static final StreamCodec<RegistryFriendlyByteBuf, TrafficCounts> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, TrafficCounts::sent,
                ByteBufCodecs.VAR_LONG, TrafficCounts::received,
                ByteBufCodecs.VAR_LONG, TrafficCounts::relayed,
                TrafficCounts::new);

        public TrafficCounts plusSent(long count) {
            return new TrafficCounts(sent + count, received, relayed);
        }

        public TrafficCounts plusReceived(long count) {
            return new TrafficCounts(sent, received + count, relayed);
        }

        public TrafficCounts plusRelayed(long count) {
            return new TrafficCounts(sent, received, relayed + count);
        }
    }
}
