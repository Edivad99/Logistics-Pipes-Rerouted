package logisticspipes.world.level.block.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.client.renderer.hud.HUDPowerLevel;
import logisticspipes.client.renderer.hud.LogisticsHUDRenderer;
import logisticspipes.interfaces.IBlockEntityMenuProvider;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.network.to_client.block.PowerJunctionLevelMessage;
import logisticspipes.network.to_server.block.BlockHudWatchMessage;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.world.inventory.PowerJunctionMenu;

@CCType(name = "LogisticsPowerJunction")
public class LogisticsPowerJunctionBlockEntity extends LogisticsSolidBlockEntity
    implements IBlockEntityMenuProvider, ILogisticsPowerProvider, IPowerLevelDisplay, IScreenOpenController,
    IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

    public final static int MAX_STORAGE = 4_000_000;

    private final PlayerCollectionList guiListener = new PlayerCollectionList();
    private final PlayerCollectionList watcherList = new PlayerCollectionList();
    private final IHeadUpDisplayRenderer HUD;

    /**
     * The FE the junction holds, which the pipes on its network draw on one for one.
     */
    private final SimpleEnergyHandler storage = new SimpleEnergyHandler(MAX_STORAGE) {

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /**
     * {@link #storage} as neighbors see it: they can fill it, but not pull from it. The junction
     * hands its energy to the LP network only.
     */
    private final EnergyHandler energyInterface = new DelegatingEnergyHandler(() -> storage) {

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    private int lastUpdateStorage = 0;
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
            storage.set(storage.getAmountAsInt() - cost(amount));
            return true;
        }
        return false;
    }

    @Override
    public boolean canUseEnergy(int amount, @Nullable List<Object> providersToIgnore) {
        if (providersToIgnore != null && providersToIgnore.contains(this)) {
            return false;
        }
        return storage.getAmountAsInt() >= cost(amount);
    }

    private static int cost(int amount) {
        return (int) ((amount * LPConfigs.COMMON.POWER_USAGE_MULTIPLIER.getAsDouble()) + 0.5D);
    }

    @Override
    public boolean useEnergy(int amount) {
        return useEnergy(amount, null);
    }

    public void updateClients() {
        final PowerJunctionLevelMessage message = new PowerJunctionLevelMessage(getBlockPos(), storage.getAmountAsInt());
        guiListener.send(message);
        watcherList.send(message);
        lastUpdateStorage = storage.getAmountAsInt();
    }

    @Override
    public boolean canUseEnergy(int amount) {
        return canUseEnergy(amount, null);
    }

    public void addEnergy(int amount) {
        if (level.isClientSide()) {
            return;
        }
        storage.set((int) Math.min(MAX_STORAGE, storage.getAmountAsLong() + amount));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Saved as "powerLevel", in LP units, before the junction stored FE.
        storage.set(Math.clamp(input.getIntOr("energy", input.getIntOr("powerLevel", 0)), 0, MAX_STORAGE));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        storage.serialize(output);
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
        if (storage.getAmountAsInt() != lastUpdateStorage) {
            updateClients();
        }
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
    @CCCommand(description = "Returns the currently stored power")
    public int getPowerLevel() {
        return storage.getAmountAsInt();
    }

    @Override
    public int getDisplayPowerLevel() {
        return getPowerLevel();
    }

    @Override
    @CCCommand(description = "Returns the max. storable power")
    public int getMaxStorage() {
        return LogisticsPowerJunctionBlockEntity.MAX_STORAGE;
    }

    @Override
    public int getChargeState() {
        return (int) (storage.getAmountAsLong() * 100 / MAX_STORAGE);
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

    public void handlePowerPacket(int stored) {
        if (level.isClientSide()) {
            storage.set(stored);
        }
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
    public boolean isHUDInvalid() {
        return isRemoved();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PowerJunctionMenu(containerId, inventory, this);
    }

    public EnergyHandler getEnergyStorageCap(@Nullable Direction direction) {
        return energyInterface;
    }
}
