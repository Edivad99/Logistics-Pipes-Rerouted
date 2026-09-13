package logisticspipes.world.level.block.entity;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.connection.NeighborBlockEntityUtil;
import logisticspipes.gui.hud.HUDPowerLevel;
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
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import logisticspipes.world.WorldCoordinatesWrapper;
import logisticspipes.world.inventory.PowerProviderMenu;

@CCType(name = "LogisticsPowerProvider")
public abstract class LogisticsPowerProviderBlockEntity extends LogisticsSolidBlockEntity
    implements IBlockEntityMenuProvider, ISubSystemPowerProvider, IPowerLevelDisplay, IScreenOpenController,
    IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

    public static final int BC_COLOR = 0x00ffff;
    public static final int RF_COLOR = 0xff0000;
    public static final int IC2_COLOR = 0xffff00;

    // true if it needs more power, turns off at full, turns on at 50%.
    public boolean needMorePowerTriggerCheck = true;

    protected Map<Integer, Double> orders = new HashMap<>();
    protected BitSet reOrdered = new BitSet(ServerRouter.getBiggestSimpleID());
    protected boolean pauseRequesting = false;

    protected double internalStorage = 0;
    protected int maxMode = 1;
    private double lastUpdateStorage = 0;
    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private final PlayerCollectionList watcherList = new PlayerCollectionList();
    private final IHeadUpDisplayRenderer HUD;
    private boolean init = false;

    protected LogisticsPowerProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        HUD = new HUDPowerLevel(this);
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (!init) {
            LogisticsHUDRenderer.instance().add(this);
            init = true;
        }
        distributePower();
    }

    @Override
    public void serverTick() {
        super.serverTick();
        distributePower();
        if (internalStorage != lastUpdateStorage) {
            updateClients();
            lastUpdateStorage = internalStorage;
        }
    }

    /**
     * Runs on both sides, as it did before the tick was split in two: {@code orders} is only ever
     * filled server side, so on the client the requested total is zero and this comes down to
     * clearing an empty map.
     */
    private void distributePower() {
        pauseRequesting = false;
        double globalRequest = orders.values().stream().reduce(Double::sum).orElse(0.0);
        if (globalRequest > 0) {
            final double fullfillRatio = Math.min(1, Math.min(internalStorage, getMaxProvidePerTick()) / globalRequest);
            if (fullfillRatio > 0) {
                final Function<NeighborBlockEntity<LogisticsTileGenericPipe>, @Nullable CoreRoutedPipe> getPipe =
                    (NeighborBlockEntity<LogisticsTileGenericPipe> neighbor) -> (CoreRoutedPipe) neighbor.getBlockEntity().pipe;
                orders.entrySet().stream()
                    .map(routerIdToOrderCount -> new Pair<>(
                        SimpleServiceLocator.routerManager.getRouter(routerIdToOrderCount.getKey()),
                        Math.min(internalStorage, routerIdToOrderCount.getValue() * fullfillRatio)))
                    .filter(destinationToPower -> destinationToPower.getValue1() != null
                        && destinationToPower.getValue1().getPipe() != null)
                    .forEach(destinationToPower -> new WorldCoordinatesWrapper(this)
                        .allNeighborBlockEntities().stream()
                        .flatMap(neighbor -> NeighborBlockEntityUtil.optionalIs(neighbor, LogisticsTileGenericPipe.class)
                            .map(Stream::of).orElseGet(Stream::empty))
                        .filter(neighbor -> neighbor.getBlockEntity().pipe instanceof CoreRoutedPipe &&
                            !getPipe.apply(neighbor).stillNeedReplace())
                        .flatMap(neighbor -> getPipe.apply(neighbor).getRouter()
                            .getDistanceTo(destinationToPower.getValue1()).stream()
                            .map(exitRoute -> new Pair<>(neighbor, exitRoute)))
                        .filter(neighborToExit ->
                            neighborToExit.getValue2().containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom) &&
                                neighborToExit.getValue2().filters.stream().noneMatch(IFilter::blockPower))
                        .findFirst()
                        .ifPresent(neighborToSource -> {
                            CoreRoutedPipe sourcePipe = getPipe.apply(neighborToSource.getValue1());
                            if (sourcePipe.isInitialized()) {
                                sourcePipe.getContainer().addLaser(neighborToSource.getValue1().getOurDirection(), 1,
                                    getLaserColor(), true, true);
                            }
                            sendPowerLaserPackets(sourcePipe.getRouter(), destinationToPower.getValue1(),
                                neighborToSource.getValue2().exitOrientation,
                                neighborToSource.getValue2().exitOrientation != neighborToSource.getValue1()
                                    .getDirection());
                            internalStorage -= destinationToPower.getValue2();
                            if (internalStorage <= 0) {
                                internalStorage = 0; // because calculations with floats
                            }
                            handlePower(destinationToPower.getValue1().getPipe(), destinationToPower.getValue2());
                        }));
            }
        }
        orders.clear();
    }

    protected abstract void handlePower(CoreRoutedPipe pipe, double toSend);

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
                    PipeRoutingConnectionType.canPowerSubSystemFrom)) { // Find only result (caused by only straight connections)
                    int distance = part.getValue1().getDistanceToNextPowerPipe(exit.exitOrientation);
                    CoreRoutedPipe pipe = part.getValue1().getPipe();
                    if (pipe != null && pipe.isInitialized()) {
                        pipe.getContainer().addLaser(exit.exitOrientation, distance, getLaserColor(), false,
                            part.getValue3());
                    }
                    IRouter nextRouter = exit.destination; // Use new sourceRouter
                    if (nextRouter == destinationRouter) {
                        return;
                    }
                    outerRouters:
                    for (ExitRoute newExit : nextRouter.getDistanceTo(destinationRouter)) {
                        if (newExit.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom)) {
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

    protected abstract double getMaxProvidePerTick();

    @CCCommand(description = "Returns the color for the power provided by this power provider")
    protected abstract int getLaserColor();

    @Override
    @CCCommand(description = "Returns the max. amount of storable power")
    public abstract int getMaxStorage();

    @Override
    @CCCommand(description = "Returns the power type stored in this power provider")
    public abstract String getBrand();

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
    public void requestPower(int destination, double amount) {
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
    @CCCommand(description = "Returns the current power level for this power provider")
    public double getPowerLevel() {
        return lastUpdateStorage;
    }

    @Override
    public boolean usePaused() {
        return pauseRequesting;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        internalStorage = input.getDoubleOr("internalStorage", 0.0);
        maxMode = input.getIntOr("maxMode", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("internalStorageDouble", internalStorage);
        output.putInt("maxMode", maxMode);
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
        final PowerProviderLevelMessage message = new PowerProviderLevelMessage(getBlockPos(), internalStorage);
        guiListener.send(message);
        watcherList.send(message);
    }

    public void handlePowerPacket(double d) {
        if (level.isClientSide()) {
            internalStorage = d;
        }
    }

    @Override
    public int getChargeState() {
        return (int) Math.min(100F, internalStorage * 100 / getMaxStorage());
    }

    @Override
    public int getDisplayPowerLevel() {
        return internalStorage > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) internalStorage;
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
}
