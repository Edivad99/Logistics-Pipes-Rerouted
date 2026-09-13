package network.rs485.logisticspipes.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.api.property.NullableEnumProperty;
import logisticspipes.api.property.Property;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReceiver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.modules.AsyncModule;
import logisticspipes.modules.PipeServiceProviderUtil;
import logisticspipes.modules.SneakyDirection;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.SneakyDirectionMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.world.inventory.SneakyDirectionMenu;

public class AsyncExtractorModule extends AsyncModule<ExtractorJob, @Nullable Void>
    implements IModuleMenuProvider, SneakyDirection, IClientInformationProvider, IHUDModuleHandler,
    IModuleWatchReceiver {

    private static final String NAME = "extractor";

    private final Predicate<ItemStack> inverseFilter;
    private final NullableEnumProperty<Direction> sneakyDirectionProp =
        new NullableEnumProperty<>(null, "sneakydirection", Direction.values());
    private final IHUDModuleRenderer hudRenderer = new HUDAsyncExtractor(this);
    private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

    private @Nullable ExtractorJob currentExtraction;

    public AsyncExtractorModule() {
        this(ItemStack::isEmpty);
    }

    public AsyncExtractorModule(Predicate<ItemStack> inverseFilter) {
        this.inverseFilter = inverseFilter;
    }

    public static String getName() {
        return NAME;
    }

    @Override
    public List<Property<?>> getProperties() {
        return List.of(sneakyDirectionProp);
    }

    @Override
    public @Nullable Direction getSneakyDirection() {
        return sneakyDirectionProp.getValue();
    }

    @Override
    public void setSneakyDirection(@Nullable Direction direction) {
        sneakyDirectionProp.setValue(direction);
        localModeWatchers.send(new SneakyDirectionMessage(ModuleTarget.of(this), Optional.ofNullable(direction)));
    }

    boolean filtersOut(ItemStack stack) {
        return inverseFilter.test(stack);
    }

    PlayerCollectionList getLocalModeWatchers() {
        return localModeWatchers;
    }

    @Nullable ServerRouter getServerRouter() {
        IPipeServiceProvider service = this.service;
        return service != null && service.getRouter() instanceof ServerRouter serverRouter ? serverRouter : null;
    }

    @Nullable IPipeServiceProvider getPipeService() {
        return service;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
        return new SneakyDirectionMenu(containerId, inventory, target, this);
    }

    @Override
    public void writeMenuData(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.optional(Direction.STREAM_CODEC)
            .encode(buffer, Optional.ofNullable(getSneakyDirection()));
    }

    @Override
    public int getEveryNthTick() {
        return (int) (80 / Math.pow(2.0, getUpgradeManager().getActionSpeedUpgrade()))
            + LPConfigs.COMMON.MINIMUM_JOB_TICK_LENGTH.getAsInt();
    }

    public int getStacksToExtract() {
        return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
    }

    public int getItemsToExtract() {
        ISlotUpgradeManager upgrades = getUpgradeManager();
        return Math.max(4 * upgrades.getItemExtractionUpgrade() + 64 * upgrades.getItemStackExtractionUpgrade(), 1);
    }

    int getEnergyPerItem() {
        ISlotUpgradeManager upgrades = getUpgradeManager();
        return (int) (5 * Math.pow(1.1, upgrades.getItemExtractionUpgrade())
            * Math.pow(1.2, upgrades.getItemStackExtractionUpgrade()));
    }

    CoreRoutedPipe.ItemSendMode getItemSendMode() {
        return getUpgradeManager().getItemExtractionUpgrade() > 0
            ? CoreRoutedPipe.ItemSendMode.Fast
            : CoreRoutedPipe.ItemSendMode.Normal;
    }

    private @Nullable IInventoryUtil getConnectedInventory() {
        IPipeServiceProvider service = this.service;
        if (service == null) {
            return null;
        }
        List<@Nullable IInventoryUtil> inventories =
            PipeServiceProviderUtil.availableSneakyInventories(service, getSneakyDirection());
        return inventories.isEmpty() ? null : inventories.getFirst();
    }

    @Override
    public String getLPName() {
        return NAME;
    }

    @Override
    public ExtractorJob jobSetup() {
        ExtractorJob job = new ExtractorJob(this, this::getConnectedInventory);
        currentExtraction = job;
        job.runSyncWork();
        return job;
    }

    @Override
    public void runSyncWork() {
        ExtractorJob job = currentExtraction;
        if (job != null) {
            job.runSyncWork();
        }
    }

    @Override
    public @Nullable Void tickAsync(ExtractorJob setupObject) {
        setupObject.runAsyncWork();
        return null;
    }

    @Override
    public void completeJob(@Nullable Void result) {
        ServerRouter serverRouter = getServerRouter();
        IInventoryUtil inventory = getConnectedInventory();
        ExtractorJob job = currentExtraction;
        if (serverRouter == null || inventory == null || job == null) {
            return;
        }
        serverRouter.ensureLatestRoutingTable();
        job.extractAndSend(serverRouter, inventory);
    }

    @Override
    public boolean receivePassive() {
        return false;
    }

    @Override
    public boolean hasGenericInterests() {
        return false;
    }

    @Override
    public boolean interestedInUndamagedID() {
        return false;
    }

    @Override
    public boolean interestedInAttachedInventory() {
        return true;
    }

    @Override
    public List<String> getClientInformation() {
        Direction sneakyDirection = getSneakyDirection();
        List<String> clientInformation = new ArrayList<>();
        clientInformation.add("Extraction: " + (sneakyDirection == null ? "DEFAULT" : sneakyDirection.name()));
        return clientInformation;
    }

    @Override
    public IHUDModuleRenderer getHUDRenderer() {
        return hudRenderer;
    }

    @Override
    public void startWatching(Player player) {
        localModeWatchers.add(player);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                new SneakyDirectionMessage(ModuleTarget.of(this), Optional.ofNullable(getSneakyDirection())));
        }
    }

    @Override
    public void stopWatching(Player player) {
        if (localModeWatchers.contains(player)) {
            localModeWatchers.remove(player);
        }
    }

    public static class HUDAsyncExtractor implements IHUDModuleRenderer {

        private final AsyncExtractorModule module;

        public HUDAsyncExtractor(AsyncExtractorModule module) {
            this.module = module;
        }

        @Override
        public void renderContent(HUDDrawContext context, boolean shifted) {
            // TODO: deferred -- this panel has never drawn anything; the sneaky direction still
            // needs a line of text through context.drawString.
        }

        @Override
        public List<IHUDButton> getButtons() {
            return List.of();
        }
    }
}
