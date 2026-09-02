package logisticspipes.world.level.block.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenController;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerJunctionGui;
import logisticspipes.network.to_client.PowerJunctionLevelMessage;
import logisticspipes.network.to_server.BlockHudWatchMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.utils.PlayerCollectionList;

@CCType(name = "LogisticsPowerJunction")
public class LogisticsPowerJunctionBlockEntity extends LogisticsSolidBlockEntity
    implements IGuiTileEntity, ILogisticsPowerProvider, IPowerLevelDisplay, IGuiOpenController,
    IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

    public final static int MAX_STORAGE = 2_000_000;
    private final static int IC_2_MULTIPLIER = 2;
    private final static int FE_DIVISOR = 2;
    private final static int MJ_MULTIPLIER = 5;

    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private final PlayerCollectionList watcherList = new PlayerCollectionList();
    private final IHeadUpDisplayRenderer HUD;
    private final EnergyHandler energyStorage = new JunctionEnergyHandler();

    /**
     * The junction's energy capability: FE in, nothing out.
     *
     * <p>Cannot be one of NeoForge's ready-made handlers, {@link net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler}
     * included, because it does not own its storage -- it is a view onto the block's LP-unit
     * {@code internalStorage} plus {@code internalFEBuffer}, the sub-LP remainder, converted through
     * {@link #FE_DIVISOR}.</p>
     *
     * <p>The old {@code IEnergyStorage} had a {@code simulate} flag; the transfer API has
     * transactions instead, so the two fields are snapshotted before the first write of each
     * transaction depth and restored if it aborts. That is strictly better than a simulate flag:
     * the accounting is written once rather than duplicated across a dry run and a real one.</p>
     */
    private class JunctionEnergyHandler extends SnapshotJournal<JunctionEnergyHandler.State> implements EnergyHandler {

        /** Everything {@link #addEnergy} touches, so that an abort leaves no trace at all. */
        private record State(int storage, int feBuffer, boolean needMorePower) {
        }

        @Override
        protected State createSnapshot() {
            return new State(internalStorage, internalFEBuffer, needMorePowerTriggerCheck);
        }

        @Override
        protected void revertToSnapshot(State snapshot) {
            internalStorage = snapshot.storage();
            internalFEBuffer = snapshot.feBuffer();
            needMorePowerTriggerCheck = snapshot.needMorePower();
        }

        @Override
        public long getAmountAsLong() {
            return (long) internalStorage * LogisticsPowerJunctionBlockEntity.FE_DIVISOR + internalFEBuffer;
        }

        @Override
        public long getCapacityAsLong() {
            return (long) LogisticsPowerJunctionBlockEntity.MAX_STORAGE * LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || freeSpace() < 1) {
                return 0;
            }
            final int feSpace = freeSpace() * LogisticsPowerJunctionBlockEntity.FE_DIVISOR - internalFEBuffer;
            final int feToTake = Math.min(amount, feSpace);
            if (feToTake <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            addEnergy(feToTake / LogisticsPowerJunctionBlockEntity.FE_DIVISOR);
            internalFEBuffer += feToTake % LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
            if (internalFEBuffer >= LogisticsPowerJunctionBlockEntity.FE_DIVISOR) {
                addEnergy(1);
                internalFEBuffer -= LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
            }
            return feToTake;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            // The junction hands its power to the LP network, never back out through the capability.
            return 0;
        }
    }


    // true if it needs more power, turns off at full, turns on at 50%.
    public boolean needMorePowerTriggerCheck = true;
    private int internalStorage = 0;
    private int lastUpdateStorage = 0;
    //small buffer to hold a fractional LP worth of FE
    private int internalFEBuffer = 0;
    private boolean init = false;

    public LogisticsPowerJunctionBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.POWER_JUNCTION.get(), pos, state);
        HUD = new HUDPowerLevel(this);
    }

    @Override
    public boolean useEnergy(int amount, @Nullable List<Object> providersToIgnore) {
        if (providersToIgnore != null && providersToIgnore.contains(this)) {
            return false;
        }
        if (canUseEnergy(amount, null)) {
            this.setChanged();
            internalStorage -= (int) ((amount * LPConfigs.COMMON.POWER_USAGE_MULTIPLIER.getAsDouble()) + 0.5D);
            if (internalStorage < LogisticsPowerJunctionBlockEntity.MAX_STORAGE / 2) {
                needMorePowerTriggerCheck = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canUseEnergy(int amount, @Nullable List<Object> providersToIgnore) {
        if (providersToIgnore != null && providersToIgnore.contains(this)) {
            return false;
        }
        return internalStorage >= (int) ((amount * LPConfigs.COMMON.POWER_USAGE_MULTIPLIER.getAsDouble()) + 0.5D);
    }

    @Override
    public boolean useEnergy(int amount) {
        return useEnergy(amount, null);
    }

    private int freeSpace() {
        return LogisticsPowerJunctionBlockEntity.MAX_STORAGE - internalStorage;
    }

    public void updateClients() {
        final PowerJunctionLevelMessage message = new PowerJunctionLevelMessage(getBlockPos(), internalStorage);
        guiListener.send(message);
        watcherList.send(message);
        lastUpdateStorage = internalStorage;
    }

    @Override
    public boolean canUseEnergy(int amount) {
        return canUseEnergy(amount, null);
    }

    public void addEnergy(int amount) {
        if (MainProxy.isClient(getWorld())) {
            return;
        }
        internalStorage += amount;
        if (internalStorage > LogisticsPowerJunctionBlockEntity.MAX_STORAGE) {
            internalStorage = LogisticsPowerJunctionBlockEntity.MAX_STORAGE;
        }
        if (internalStorage == LogisticsPowerJunctionBlockEntity.MAX_STORAGE) {
            needMorePowerTriggerCheck = false;
        }
        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        internalStorage = input.getIntOr("powerLevel", 0);
        needMorePowerTriggerCheck = input.getBooleanOr("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("powerLevel", internalStorage);
        output.putBoolean("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
    }

    @Override
    public void update() {
        super.update();
        if (MainProxy.isServer(getWorld())) {
            if (internalStorage != lastUpdateStorage) {
                updateClients();
            }
        }
        if (!init) {
            if (MainProxy.isClient(getWorld())) {
                LogisticsHUDRenderer.instance().add(this);
            }
            init = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (MainProxy.isClient(getWorld())) {
            LogisticsHUDRenderer.instance().remove(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (MainProxy.isClient(getWorld())) {
            init = false;
        }
    }

    // onChunkUnload removed in 1.20.1 — setRemoved() covers this case

    @Override
    @CCCommand(description = "Returns the currently stored power")
    public int getPowerLevel() {
        return internalStorage;
    }

    @Override
    public int getDisplayPowerLevel() {
        return getPowerLevel();
    }

    @Override
    public String getBrand() {
        return "LP";
    }

    @Override
    @CCCommand(description = "Returns the max. storable power")
    public int getMaxStorage() {
        return LogisticsPowerJunctionBlockEntity.MAX_STORAGE;
    }

    @Override
    public int getChargeState() {
        return internalStorage * 100 / LogisticsPowerJunctionBlockEntity.MAX_STORAGE;
    }

    @Override
    public void guiOpenedByPlayer(Player player) {
        guiListener.add(player);
        updateClients();
    }

    @Override
    public void guiClosedByPlayer(Player player) {
        guiListener.remove(player);
    }

    public void handlePowerPacket(int integer) {
        if (MainProxy.isClient(getWorld())) {
            internalStorage = integer;
        }
    }

    @Override
    public IHeadUpDisplayRenderer getRenderer() {
        return HUD;
    }

    @Override
    public Level getLevelForHUD() {
        return getWorld();
    }

    @Override
    public int getX() {
        return getBlockPos().getX();
    }

    @Override
    public int getY() {
        return getBlockPos().getY();
    }

    @Override
    public int getZ() {
        return getBlockPos().getZ();
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
        return getWorld().getBlockEntity(getBlockPos()) == this;
    }

    @Override
    public boolean isHUDInvalid() {
        return isRemoved();
    }

    @Override
    public CoordinatesGuiProvider getGuiProvider() {
        return NewGuiHandler.getGui(PowerJunctionGui.class);
    }

    public EnergyHandler getEnergyStorageCap(@Nullable Direction direction) {
        return energyStorage;
    }
}
