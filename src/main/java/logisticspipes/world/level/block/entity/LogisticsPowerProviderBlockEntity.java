package logisticspipes.world.level.block.entity;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.client.renderer.hud.HUDPowerLevel;
import logisticspipes.client.renderer.hud.LogisticsHUDRenderer;
import logisticspipes.connection.LPNeighborBlockEntity;
import logisticspipes.interfaces.IBlockEntityMenuProvider;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.to_client.block.PowerProviderLevelMessage;
import logisticspipes.network.to_server.block.BlockHudWatchMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.tuples.Triplet;
import logisticspipes.world.WorldCoordinatesWrapper;
import logisticspipes.world.inventory.PowerProviderMenu;

/**
 * Stores FE and hands it to the pipes on the network that carry an FE power supplier upgrade.
 *
 * <p>Pipes order what they are missing through {@link #requestPower}; every tick the orders are
 * filled, scaled down when the provider cannot cover them all, and the energy is sent along the
 * route with a laser to show it.</p>
 */
public class LogisticsPowerProviderBlockEntity extends LogisticsSolidBlockEntity
    implements IBlockEntityMenuProvider, ISubSystemPowerProvider, IPowerLevelDisplay, IScreenOpenController,
    IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

    public static final int FE_COLOR = 0xff0000;
    public static final int MAX_STORAGE = 10_000_000;
    /**
     * The most a single insertion takes in, which bounds what a cable or a pull from a neighbor
     * moves per tick.
     */
    private static final int MAX_INSERT = 10_000;
    private static final int MAX_PROVIDE_PER_TICK = 10_000;

    private final SimpleEnergyHandler storage =
        new SimpleEnergyHandler(MAX_STORAGE, MAX_INSERT, MAX_PROVIDE_PER_TICK) {

            @Override
            protected void onEnergyChanged(int previousAmount) {
                setChanged();
            }
        };

    /**
     * {@link #storage} as neighbors see it: they can fill it, but not pull from it. The provider
     * hands its energy to the network itself.
     */
    private final EnergyHandler energyInterface = new DelegatingEnergyHandler(() -> storage) {

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    private final Map<Integer, Integer> orders = new HashMap<>();
    private final BitSet reOrdered = new BitSet(ServerRouter.getBiggestSimpleID());
    private boolean pauseRequesting = false;

    private long lastSyncedAmount = 0;
    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private final PlayerCollectionList watcherList = new PlayerCollectionList();
    private final IHeadUpDisplayRenderer HUD;
    private boolean init = false;

    public LogisticsPowerProviderBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.POWER_PROVIDER_FE.get(), pos, state);
        HUD = new HUDPowerLevel(this);
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (!init) {
            LogisticsHUDRenderer.instance().add(this);
            init = true;
        }
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (LPConfigs.COMMON.POWER_SOURCE_MODE.get() == LPConfigs.PowerSourceMode.ADJACENT) {
            pullFromNeighbors();
        }
        distributePower();
        if (storage.getAmountAsLong() != lastSyncedAmount) {
            updateClients();
            lastSyncedAmount = storage.getAmountAsLong();
        }
    }

    private void pullFromNeighbors() {
        for (Direction direction : Direction.values()) {
            if (EnergyHandlerUtil.isFull(storage)) {
                return;
            }
            BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(direction));
            if (neighbor == null) {
                continue;
            }
            EnergyHandler neighborStorage = level.getCapability(Capabilities.Energy.BLOCK, neighbor.getBlockPos(),
                neighbor.getBlockState(), neighbor, direction.getOpposite());
            EnergyHandlerUtil.move(neighborStorage, storage, MAX_INSERT, null);
        }
    }

    /**
     * Fills this tick's orders, all scaled by the same ratio when the provider cannot cover them.
     */
    private void distributePower() {
        pauseRequesting = false;
        long requested = orders.values().stream().mapToLong(Integer::longValue).sum();
        if (requested > 0) {
            double ratio = Math.min(1, (double) Math.min(storage.getAmountAsLong(), MAX_PROVIDE_PER_TICK) / requested);
            for (Map.Entry<Integer, Integer> order : orders.entrySet()) {
                IRouter destination = RouterManager.get().getRouter(order.getKey());
                int amount = (int) (order.getValue() * ratio);
                if (destination != null && destination.getPipe() != null && amount > 0) {
                    deliver(destination, amount);
                }
            }
        }
        orders.clear();
    }

    /**
     * Sends {@code amount} to {@code destination} through the first neighboring pipe with a route to
     * it that power may take.
     */
    private void deliver(IRouter destination, int amount) {
        for (LPNeighborBlockEntity<BlockEntity> neighbor : new WorldCoordinatesWrapper(this).allNeighborBlockEntities()) {
            if (!(neighbor.getBlockEntity() instanceof LogisticsGenericPipeBlockEntity pipeBlockEntity)
                || !(pipeBlockEntity.pipe instanceof CoreRoutedPipe sourcePipe) || sourcePipe.stillNeedReplace()) {
                continue;
            }
            for (ExitRoute route : sourcePipe.getRouter().getDistanceTo(destination)) {
                if (!route.containsFlag(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM)
                    || route.filters.stream().anyMatch(IFilter::blockPower)) {
                    continue;
                }
                int extracted;
                try (Transaction transaction = Transaction.openRoot()) {
                    extracted = storage.extract(amount, transaction);
                    transaction.commit();
                }
                if (extracted == 0) {
                    return;
                }
                if (sourcePipe.isInitialized()) {
                    sourcePipe.getContainer().addLaser(neighbor.getOurDirection(), 1, FE_COLOR, true, true);
                }
                sendPowerLaserPackets(sourcePipe.getRouter(), destination, route.exitOrientation,
                    route.exitOrientation != neighbor.getDirection());
                destination.getPipe().handleFEPowerArrival(extracted);
                return;
            }
        }
    }

    private void sendPowerLaserPackets(IRouter sourceRouter, IRouter destinationRouter, Direction exitOrientation,
        boolean addBall) {
        if (sourceRouter == destinationRouter) {
            return;
        }
        LinkedList<Triplet<IRouter, Direction, Boolean>> todo = new LinkedList<>();
        todo.add(new Triplet<>(sourceRouter, exitOrientation, addBall));
        while (!todo.isEmpty()) {
            Triplet<IRouter, Direction, Boolean> part = todo.pollFirst();
            List<ExitRoute> exits = part.getValue1().getRoutersOnSide(part.getValue2());
            for (ExitRoute exit : exits) {
                if (exit.containsFlag(
                    PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM)) { // Find only result (caused by only straight connections)
                    int distance = part.getValue1().getDistanceToNextPowerPipe(exit.exitOrientation);
                    CoreRoutedPipe pipe = part.getValue1().getPipe();
                    if (pipe != null && pipe.isInitialized()) {
                        pipe.getContainer().addLaser(exit.exitOrientation, distance, FE_COLOR, false,
                            part.getValue3());
                    }
                    IRouter nextRouter = exit.destination; // Use new sourceRouter
                    if (nextRouter == destinationRouter) {
                        return;
                    }
                    outerRouters:
                    for (ExitRoute newExit : nextRouter.getDistanceTo(destinationRouter)) {
                        if (newExit.containsFlag(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM)) {
                            for (IFilter filter : newExit.filters) {
                                if (filter.blockPower()) {
                                    continue outerRouters;
                                }
                            }
                            todo.addLast(new Triplet<>(nextRouter, newExit.exitOrientation,
                                newExit.exitOrientation != exit.exitOrientation));
                        }
                    }
                }
            }
        }
    }

    public int getLaserColor() {
        return FE_COLOR;
    }

    @Override
    public int getMaxStorage() {
        return MAX_STORAGE;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level.isClientSide()) {
            LogisticsHUDRenderer.instance().remove(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide()) {
            init = false;
        }
    }

    @Override
    public void requestPower(int destination, int amount) {
        if (pauseRequesting) {
            return;
        }
        if (orders.containsKey(destination)) {
            if (reOrdered.get(destination)) {
                pauseRequesting = true;
                reOrdered.clear();
            } else {
                reOrdered.set(destination);
            }
        } else {
            reOrdered.clear();
        }
        orders.put(destination, amount);
    }

    @Override
    public int getPowerLevel() {
        return storage.getAmountAsInt();
    }

    @Override
    public boolean usePaused() {
        return pauseRequesting;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storage.deserialize(input);
        // Older saves kept the stored energy apart, as a double, with "energy" only holding the
        // small buffer cables pushed into.
        double legacy = input.getDoubleOr("internalStorage", 0);
        if (legacy > 0) {
            storage.set((int) Math.min(MAX_STORAGE, storage.getAmountAsLong() + (long) legacy));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        storage.serialize(output);
    }

    @Override
    public IHeadUpDisplayRenderer getRenderer() {
        return HUD;
    }

    @Override
    public @Nullable Level getLevelForHUD() {
        return level;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public void startWatching() {
        ClientPacketDistributor.sendToServer(new BlockHudWatchMessage(getBlockPos(), true));
    }

    @Override
    public void stopWatching() {
        ClientPacketDistributor.sendToServer(new BlockHudWatchMessage(getBlockPos(), false));
    }

    @Override
    public void playerStartWatching(Player player) {
        watcherList.add(player);
        updateClients();
    }

    @Override
    public void playerStopWatching(Player player) {
        watcherList.remove(player);
    }

    @Override
    public boolean isHUDExistent() {
        return level.getBlockEntity(getBlockPos()) == this;
    }

    @Override
    public void screenOpenedByPlayer(Player player) {
        guiListener.add(player);
        updateClients();
    }

    @Override
    public void screenClosedByPlayer(Player player) {
        guiListener.remove(player);
    }

    public void updateClients() {
        final PowerProviderLevelMessage message = new PowerProviderLevelMessage(getBlockPos(), storage.getAmountAsInt());
        guiListener.send(message);
        watcherList.send(message);
    }

    public void handlePowerPacket(int stored) {
        if (level.isClientSide()) {
            storage.set(stored);
        }
    }

    @Override
    public int getChargeState() {
        return (int) (storage.getAmountAsLong() * 100 / MAX_STORAGE);
    }

    @Override
    public int getDisplayPowerLevel() {
        return storage.getAmountAsInt();
    }

    @Override
    public boolean isHUDInvalid() {
        return isRemoved();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PowerProviderMenu(containerId, inventory, this);
    }

    public EnergyHandler getEnergyStorageCap(@Nullable Direction side) {
        return energyInterface;
    }
}
