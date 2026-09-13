package network.rs485.logisticspipes.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.BooleanProperty;
import logisticspipes.api.property.Property;
import logisticspipes.gui.hud.modules.HUDAdvancedExtractor;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.ILevelProvider;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReceiver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.modules.AsyncModule;
import logisticspipes.modules.SimpleFilter;
import logisticspipes.modules.SneakyDirection;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.AdvancedExtractorIncludeMessage;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.inventory.AdvancedExtractorMenu;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.util.ItemKt;

public class AsyncAdvancedExtractor extends AsyncModule<ExtractorJob, @Nullable Void>
    implements SimpleFilter, SneakyDirection, IClientInformationProvider, IHUDModuleHandler,
    IModuleWatchReceiver, IModuleInventoryReceive, ISimpleInventoryEventHandler, IModuleMenuProvider {

    private static final String NAME = "extractor_advanced";

    private final ItemIdentifierInventoryProperty filterInventory =
        new ItemIdentifierInventoryProperty(new ItemIdentifierInventory(9, "Item list", 1), "filterInv");
    private final BooleanProperty itemsIncluded = new BooleanProperty(true, "itemsIncluded");
    private final IHUDModuleRenderer hud = new HUDAdvancedExtractor(this);
    private final AsyncExtractorModule extractor = new AsyncExtractorModule(this::isFilteredOut);

    public static String getName() {
        return NAME;
    }

    public BooleanProperty getItemsIncluded() {
        return itemsIncluded;
    }

    /** Keeps what the filter list says to keep, which the "included" flag turns around. */
    private boolean isFilteredOut(ItemStack stack) {
        return stack.isEmpty()
            || itemsIncluded.getValue() != ItemKt.matchingSequence(filterInventory, stack).iterator().hasNext();
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> properties = new ArrayList<>(extractor.getProperties());
        properties.add(filterInventory);
        properties.add(itemsIncluded);
        return properties;
    }

    @Override
    public @Nullable Direction getSneakyDirection() {
        return extractor.getSneakyDirection();
    }

    @Override
    public void setSneakyDirection(@Nullable Direction direction) {
        extractor.setSneakyDirection(direction);
    }

    @Override
    public int getEveryNthTick() {
        return extractor.getEveryNthTick();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
        return new AdvancedExtractorMenu(containerId, inventory, target, this);
    }

    @Override
    public void writeMenuData(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(itemsIncluded.getValue());
    }

    @Override
    public void finishInit() {
        boolean wasInitialized = initialized;
        super.finishInit();
        if (wasInitialized || service == null) {
            return;
        }
        ILevelProvider worldProvider = this.worldProvider;
        Level level = worldProvider == null ? null : worldProvider.getLevel();
        if (level != null && !level.isClientSide()) {
            itemsIncluded.addObserver(property -> extractor.getLocalModeWatchers()
                .send(new AdvancedExtractorIncludeMessage(ModuleTarget.of(this), property.copyValue())));
        }
    }

    @Override
    public String getLPName() {
        return NAME;
    }

    @Override
    public void registerHandler(@Nullable ILevelProvider world, @Nullable IPipeServiceProvider service) {
        super.registerHandler(world, service);
        extractor.registerHandler(world, service);
    }

    @Override
    public void registerPosition(ModulePositionType slot, int positionInt) {
        super.registerPosition(slot, positionInt);
        extractor.registerPosition(slot, positionInt);
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
        return false;
    }

    @Override
    public ExtractorJob jobSetup() {
        return extractor.jobSetup();
    }

    @Override
    public void completeJob(@Nullable Void result) {
        extractor.completeJob(result);
    }

    @Override
    public @Nullable Void tickAsync(ExtractorJob setupObject) {
        return extractor.tickAsync(setupObject);
    }

    @Override
    public void runSyncWork() {
        extractor.runSyncWork();
    }

    @CCCommand(description = "Returns the FilterInventory of this Module")
    @Override
    public IItemIdentifierInventory getFilterInventory() {
        return filterInventory;
    }

    @Override
    public void handleInvContent(Collection<@Nullable ItemIdentifierStack> allItems) {
        filterInventory.handleItemIdentifierList(allItems);
    }

    @Override
    public void InventoryChanged(Container inventory) {
        Level world = getWorld();
        if (world != null && !world.isClientSide()) {
            extractor.getLocalModeWatchers().send(new ModuleInventoryMessage(ModuleTarget.of(this),
                ItemIdentifierStack.getListFromInventory(inventory)));
        }
    }

    @Override
    public List<String> getClientInformation() {
        List<String> clientInformation = new ArrayList<>(extractor.getClientInformation());
        clientInformation.add(itemsIncluded.getValue() ? "Included" : "Excluded");
        // The pair the tooltip looks for: it turns the filter into an item grid instead of the
        // raw stack strings addAll(filterInventory.clientInformation) used to leave here.
        clientInformation.add("<inventory>");
        clientInformation.add("<that>" + filterInventory.getTagKey());
        return clientInformation;
    }

    @Override
    public void startWatching(Player player) {
        extractor.startWatching(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModuleTarget target = ModuleTarget.of(this);
            PacketDistributor.sendToPlayer(serverPlayer,
                new ModuleInventoryMessage(target, ItemIdentifierStack.getListFromInventory(filterInventory)));
            PacketDistributor.sendToPlayer(serverPlayer,
                new AdvancedExtractorIncludeMessage(target, itemsIncluded.getValue()));
        }
    }

    @Override
    public void stopWatching(Player player) {
        extractor.stopWatching(player);
    }

    @Override
    public IHUDModuleRenderer getHUDRenderer() {
        return hud;
    }
}
