package logisticspipes.world.level.block.entity;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.energy.IEnergyStorage;

import logisticspipes.LPConfigs;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerJunctionGui;
import logisticspipes.network.packets.block.PowerJunctionLevel;
import logisticspipes.network.packets.hud.HUDStartBlockWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopBlockWatchingPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.utils.PlayerCollectionList;

@CCType(name = "LogisticsPowerJunction")
public class LogisticsPowerJunctionBlockEntity extends LogisticsSolidBlockEntity
    implements IGuiTileEntity, ILogisticsPowerProvider, IPowerLevelDisplay, IGuiOpenControler,
    IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

    public final static int MAX_STORAGE = 2_000_000;
    private final static int IC_2_MULTIPLIER = 2;
    private final static int FE_DIVISOR = 2;
    private final static int MJ_MULTIPLIER = 5;

    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private final PlayerCollectionList watcherList = new PlayerCollectionList();
    private final IHeadUpDisplayRenderer HUD;
    private final IEnergyStorage energyStorage = new IEnergyStorage() {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (freeSpace() < 1) {
                return 0;
            }
            final int FE_Space = freeSpace() * LogisticsPowerJunctionBlockEntity.FE_DIVISOR - internalFEBuffer;
            final int FE_ToTake = Math.min(maxReceive, FE_Space);
            if (!simulate) {
                addEnergy(FE_ToTake / LogisticsPowerJunctionBlockEntity.FE_DIVISOR);
                internalFEBuffer += FE_ToTake % LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
                if (internalFEBuffer >= LogisticsPowerJunctionBlockEntity.FE_DIVISOR) {
                    addEnergy(1);
                    internalFEBuffer -= LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
                }
            }
            return FE_ToTake;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return internalStorage * LogisticsPowerJunctionBlockEntity.FE_DIVISOR + internalFEBuffer;
        }

        @Override
        public int getMaxEnergyStored() {
            return LogisticsPowerJunctionBlockEntity.MAX_STORAGE * LogisticsPowerJunctionBlockEntity.FE_DIVISOR;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

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
        MainProxy.sendToPlayerList(
            PacketHandler.getPacket(PowerJunctionLevel.class).putInt(internalStorage).setBlockPos(getBlockPos()),
            guiListener);
        MainProxy.sendToPlayerList(
            PacketHandler.getPacket(PowerJunctionLevel.class).putInt(internalStorage).setBlockPos(getBlockPos()),
            watcherList);
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        internalStorage = tag.getIntOr("powerLevel", 0);
        if (tag.contains("needMorePowerTriggerCheck")) {
            needMorePowerTriggerCheck = tag.getBooleanOr("needMorePowerTriggerCheck", false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("powerLevel", internalStorage);
        tag.putBoolean("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
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
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(HUDStartBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
    }

    @Override
    public void stopWatching() {
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(HUDStopBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
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

    public IEnergyStorage getEnergyStorageCap(@Nullable Direction direction) {
        return this.energyStorage;
    }
}
