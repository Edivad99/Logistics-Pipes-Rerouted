package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.NeighborBlockEntity;
import logisticspipes.api.property.BooleanProperty;
import logisticspipes.api.property.EnumProperty;
import logisticspipes.api.property.NullableEnumProperty;
import logisticspipes.api.property.Property;
import logisticspipes.client.renderer.hud.modules.HUDProviderModule;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.ILegacyActiveModule;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReceiver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.inventory.IExternalCrafter;
import logisticspipes.inventory.IInventoryUtil;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.InventoryUtilFactory;
import logisticspipes.inventory.ProviderMode;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.network.to_client.module.SneakyDirectionMessage;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.property.ItemIdentifierInventoryProperty;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.ItemCraftingTemplate;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.order.ItemOrder;
import logisticspipes.request.order.ItemOrderManager;
import logisticspipes.request.order.Order;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.sink.ItemSinkFinder;
import logisticspipes.routing.sink.SinkReply;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.inventory.ProviderMenu;

/**
 * Supplies the network from the inventories next to it. When such an inventory has an autocrafting
 * system behind it, an Applied Energistics 2 network seen through an ME Interface, the provider also
 * offers to craft what that system can make: it hands the request on and sends the result once it
 * shows up in the inventory.
 */
public class ModuleProvider extends LogisticsModule implements SneakyDirection, ILegacyActiveModule,
    IClientInformationProvider, IHUDModuleHandler, IModuleWatchReceiver, IModuleInventoryReceive,
    IModuleMenuProvider, ICraftItems {

    public final ArrayList<@Nullable ItemIdentifierStack> displayList = new ArrayList<>();
    public final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
        new ItemIdentifierInventory(9, "Items to provide (or empty for all)", 1), "filterInv");
    public final BooleanProperty isActive = new BooleanProperty(false, "isActive");
    public final BooleanProperty isExclusionFilter = new BooleanProperty(false, "filterisexclude");
    public final EnumProperty<ProviderMode> providerMode =
        new EnumProperty<>(ProviderMode.DEFAULT, "extractionMode", ProviderMode.values());
    public final NullableEnumProperty<Direction> sneakyDirection =
        new NullableEnumProperty<>(null, "sneakydirection", Direction.values());
    public final ImmutableList<Property<?>> propertyList = ImmutableList.<Property<?>>builder()
        .add(filterInventory)
        .add(isActive)
        .add(isExclusionFilter)
        .add(providerMode)
        .add(sneakyDirection)
        .build();
    private final Map<ItemIdentifier, Integer> displayMap = new TreeMap<>();
    private final ArrayList<ItemIdentifierStack> oldList = new ArrayList<>();
    private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
    private final IHUDModuleRenderer HUD = new HUDProviderModule(this);
    /**
     * The request handed to the autocrafting system for each crafting order still open. Not saved:
     * like every other order, these are lost when the world is reloaded.
     */
    private final Map<ItemOrder, IExternalCrafter.Job> craftingJobs = new IdentityHashMap<>();

    public ModuleProvider() {
    }

    public static String getName() {
        return "provider";
    }

    @Override
    public String getLPName() {
        return getName();
    }

    /**
     * Returns a list of all the properties of this module.
     */
    @Override
    public List<Property<?>> getProperties() {
        return propertyList;
    }

    @Override
    public Direction getSneakyDirection() {
        return sneakyDirection.getValue();
    }

    @Override
    public void setSneakyDirection(@Nullable Direction direction) {
        sneakyDirection.setValue(direction);
        final Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            localModeWatchers.send(new SneakyDirectionMessage(ModuleTarget.of(this),
                Optional.ofNullable(sneakyDirection.getValue())));
        }
    }

    protected int neededEnergy() {
        return (int) (1 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math
            .pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()));
    }

    protected int itemsToExtract() {
        return 8 * (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
    }

    protected int stacksToExtract() {
        return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
    }

    public ItemSendMode itemSendMode() {
        return getUpgradeManager().getItemExtractionUpgrade() > 0 ? ItemSendMode.FAST : ItemSendMode.NORMAL;
    }

    @Override
    public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
        boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
        return null;
    }

    @Override
    public void tick() {
        final IPipeServiceProvider service = this.service;
        if (service == null) {
            return;
        }
        if (service.isNthTick(6)) {
            checkUpdate(null);
        }
        int itemsLeft = itemsToExtract();
        int stacksLeft = stacksToExtract();
        ItemOrder firstOrder = null;
        ItemOrder order = null;
        while (itemsLeft > 0 && stacksLeft > 0 && service.getItemOrderManager().hasOrders(ResourceType.PROVIDER) && (
            firstOrder == null || firstOrder != order)) {
            if (firstOrder == null) {
                firstOrder = order;
            }
            order = service.getItemOrderManager().peekAtTopRequest(ResourceType.PROVIDER);
            int sent = sendStack(order.getResource().stack, itemsLeft, order.getDestination().getRouter().getSimpleID(),
                order.getInformation());
            if (sent < 0) {
                break;
            }
            service.spawnParticle(Particles.VIOLET_SPARKLE, 3);
            stacksLeft -= 1;
            itemsLeft -= sent;
        }
        tickCraftingOrders(service, itemsLeft, stacksLeft);
    }

    /**
     * Moves the crafting orders along: follows the requests handed to the autocrafting system, sends
     * what it has made so far and gives up on the orders it cannot make.
     */
    private void tickCraftingOrders(IPipeServiceProvider service, int itemsLeft, int stacksLeft) {
        ItemOrderManager orderManager = service.getItemOrderManager();
        craftingJobs.keySet().removeIf(Order::isFinished);
        ItemOrder firstDeferred = null;
        while (itemsLeft > 0 && stacksLeft > 0) {
            ItemOrder order = orderManager.peekAtTopRequest(ResourceType.CRAFTING);
            if (order == null || order == firstDeferred) {
                break;
            }
            IExternalCrafter.Job job = craftingJobs.get(order);
            IExternalCrafter.State state = job == null ? IExternalCrafter.State.FAILED : job.poll();
            if (state == IExternalCrafter.State.FAILED) {
                craftingJobs.remove(order);
                orderManager.sendFailed();
                continue;
            }
            ItemIdentifier item = order.getResource().getItem();
            if (state == IExternalCrafter.State.PENDING || availableForCrafting(orderManager, item) <= 0) {
                // Not made yet: look at the next order, and stop once every one has been looked at.
                if (firstDeferred == null) {
                    firstDeferred = order;
                }
                orderManager.deferSend();
                continue;
            }
            int sent = sendStack(order.getResource().stack, itemsLeft, order.getDestination().getRouter().getSimpleID(),
                order.getInformation());
            if (sent < 0) {
                break;
            }
            service.spawnParticle(Particles.VIOLET_SPARKLE, 3);
            stacksLeft -= 1;
            itemsLeft -= sent;
        }
    }

    /**
     * How many of {@code item} the inventories hold beyond what the plain provider orders have
     * already been promised, so a crafting order does not take what was there before it.
     */
    private int availableForCrafting(ItemOrderManager orderManager, ItemIdentifier item) {
        int promised = 0;
        for (ItemOrder order : orderManager) {
            if (order.getType() == ResourceType.PROVIDER && order.getResource().getItem().equals(item)) {
                promised += order.getAmount();
            }
        }
        return getTotalItemCount(item) - promised;
    }

    public boolean filterBlocksItem(ItemIdentifier item) {
        if (filterInventory.isEmpty()) {
            return false;
        }
        boolean isFiltered = filterInventory.containsItem(item);
        return isExclusionFilter.getValue() == isFiltered;
    }

    @Override
    public void onBlockRemoval() {
        final IPipeServiceProvider service = this.service;
        if (service == null) {
            return;
        }
        while (service.getItemOrderManager().hasOrders(ResourceType.PROVIDER, ResourceType.CRAFTING)) {
            service.getItemOrderManager().sendFailed();
        }
        craftingJobs.values().forEach(IExternalCrafter.Job::cancel);
        craftingJobs.clear();
    }

    public Stream<IInventoryUtil> inventoriesWithMode() {
        final IPipeServiceProvider service = this.service;
        if (service == null) {
            return Stream.empty();
        }
        return service.getAvailableAdjacent().inventories().stream()
            .map(this::getInventoryUtilWithMode)
            .filter(Objects::nonNull);
    }

    @Override
    public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
        List<ItemIdentifier> possible = new ArrayList<>();
        if (tree.getRequestType() instanceof ItemResource itemResource) {
            possible.add(itemResource.getItem());
        } else if (tree.getRequestType() instanceof DictResource) {
            possible.addAll(
                inventoriesWithMode()
                    .map(IInventoryUtil::getItemsAndCount)
                    .flatMap(inventory -> inventory.keySet().stream())
                    .filter(item -> tree.getRequestType().matches(item, IResource.MatchSettings.NORMAL))
                    .toList()
            );
        }
        for (ItemIdentifier item : possible) {
            if (blockedByFilters(item, filters)) {
                continue; // a firewall on the way does not let it through
            }
            int canProvide = getAvailableItemCount(item);
            canProvide -= root.getAllPromissesFor((IProvideItems) service, item);
            canProvide = Math.min(canProvide, tree.getMissingAmount());
            if (canProvide < 1) {
                continue;
            }
            ItemPromise promise = new ItemPromise(item, canProvide, (IProvideItems) service,
                ResourceType.PROVIDER);
            tree.addPromise(promise);
        }
    }

    @Override
    public @Nullable Order fullFill(ItemPromise promise, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info) {
        if (this.service == null) {
            return null;
        }
        this.service.spawnParticle(Particles.WHITE_SPARKLE, 2);
        if (promise.type == ResourceType.CRAFTING) {
            ItemOrder order = this.service.getItemOrderManager()
                .addOrder(new ItemIdentifierStack(promise.item, promise.numberOfItems), destination,
                    ResourceType.CRAFTING, info);
            IExternalCrafter crafter = findCrafterFor(promise.item);
            if (crafter != null) {
                craftingJobs.put(order, crafter.startCrafting(promise.item, promise.numberOfItems));
            }
            // Without a crafter the order has no job, and the next tick turns it down.
            return order;
        }
        return this.service.getItemOrderManager()
            .addOrder(new ItemIdentifierStack(promise.item, promise.numberOfItems), destination,
                ResourceType.PROVIDER, info);
    }

    /* Crafting through an autocrafting system behind the inventory */

    /**
     * The autocrafting systems behind the inventories this provider faces.
     */
    private Stream<IExternalCrafter> externalCrafters() {
        return inventoriesWithMode().map(IInventoryUtil::getExternalCrafter).filter(Objects::nonNull);
    }

    private @Nullable IExternalCrafter findCrafterFor(ItemIdentifier item) {
        if (filterBlocksItem(item)) {
            return null;
        }
        return externalCrafters().filter(crafter -> crafter.canCraft(item)).findFirst().orElse(null);
    }

    private static @Nullable ItemIdentifier itemOf(IResource resource) {
        if (resource instanceof ItemResource itemResource) {
            return itemResource.getItem();
        }
        if (resource instanceof DictResource dictResource) {
            return dictResource.getItem();
        }
        return null;
    }

    @Override
    public boolean canCraft(IResource toCraft) {
        ItemIdentifier item = itemOf(toCraft);
        return item != null && findCrafterFor(item) != null;
    }

    @Override
    public @Nullable ICraftingTemplate addCrafting(IResource toCraft) {
        ItemIdentifier item = itemOf(toCraft);
        if (item == null) {
            return null;
        }
        IExternalCrafter crafter = findCrafterFor(item);
        return crafter == null ? null : new ExternalCraftingTemplate(item, crafter, this);
    }

    @Override
    public List<ItemIdentifierStack> getCraftedItems() {
        return craftableItems().stream().map(item -> new ItemIdentifierStack(item, 1)).toList();
    }

    /**
     * What the autocrafting systems behind the inventories can make, less what the filter blocks.
     */
    public Set<ItemIdentifier> craftableItems() {
        return externalCrafters()
            .flatMap(crafter -> crafter.getCraftables().stream())
            .filter(item -> !filterBlocksItem(item))
            .collect(Collectors.toSet());
    }

    @Override
    public void registerExtras(IPromise promise) {
        // The system makes exactly what is asked, so there is never anything left over.
    }

    @Override
    public int getTodo() {
        final IPipeServiceProvider service = this.service;
        if (service == null) {
            return 0;
        }
        int todo = 0;
        for (ItemOrder order : service.getItemOrderManager()) {
            if (order.getType() == ResourceType.CRAFTING) {
                todo += order.getAmount();
            }
        }
        return todo;
    }

    @Override
    public IRouter getRouter() {
        return Objects.requireNonNull(service, "service was null").getRouter();
    }

    @Override
    public int getID() {
        final IPipeServiceProvider service = this.service;
        return service == null ? -1 : service.getRouter().getSimpleID();
    }

    @Override
    public int compareTo(IRequestItems other) {
        return Integer.compare(getID(), other.getID());
    }

    @Override
    public void itemCouldNotBeSent(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        // Nothing is ever requested for this provider: the system brings its own ingredients.
    }

    @Override
    public void itemLost(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
    }

    @Override
    public void itemArrived(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
    }

    private int getAvailableItemCount(ItemIdentifier item) {
        if (this.service == null) {
            return 0;
        }
        return getTotalItemCount(item) - this.service.getItemOrderManager().totalItemsCountInOrders(item);
    }

    /**
     * Whether a filter between this provider and whoever asks, a firewall's, keeps {@code item} from
     * being provided.
     */
    private static boolean blockedByFilters(ItemIdentifier item, List<IFilter> filters) {
        return filters.stream().anyMatch(
            filter -> filter.isBlocked() == filter.isFilteredItem(item.getUndamaged()) || filter.blockProvider());
    }

    @Override
    public void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters) {
        if (this.service == null) {
            return;
        }
        items.putAll(
            inventoriesWithMode()
                .map(IInventoryUtil::getItemsAndCount)
                .flatMap(inventory -> inventory.entrySet().stream())
                .filter(item -> {
                    if (items.containsKey(item.getKey())) {
                        return false; // already provided by any previous module. No comparison of the amount
                    }
                    if (filterBlocksItem(item.getKey())) {
                        return false; // skip provider-filtered items
                    }
                    return !blockedByFilters(item.getKey(), filters); // skip filters-parameter-filtered items
                })
                .map(item -> Map.entry(item.getKey(), item.getValue() - this.service.getItemOrderManager()
                    .totalItemsCountInOrders(item.getKey())))
                .filter(remaining -> remaining.getValue() > 0) // reduce what has been reserved
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum)) // sum up the provided amount by the inventories
        );
    }

    // returns -1 on permanently failed, don't try another stack this tick
    // returns 0 on "unable to do this delivery"
    public int sendStack(ItemIdentifierStack stack, int maxCount, int destination, IAdditionalTargetInformation info) {
        if (this.service == null) {
            return -1;
        }

        ItemIdentifier item = stack.getItem();

        for (NeighborBlockEntity<BlockEntity> neighbor : this.service.getAvailableAdjacent().inventories()) {
            final IInventoryUtil invUtil = getInventoryUtilWithMode(neighbor);
            if (invUtil == null) {
                continue;
            }
            int available = invUtil.itemCount(item);
            if (available == 0) {
                continue;
            }

            int wanted = Math.min(available, stack.getStackSize());
            wanted = Math.min(wanted, maxCount);
            wanted = Math.min(wanted, item.getMaxStackSize());
            IRouter dRtr = RouterManager.get().getServerRouter(destination);
            if (dRtr == null) {
                this.service.getItemOrderManager().sendFailed();
                return 0;
            }
            SinkReply reply = ItemSinkFinder
                .canSink(stack.makeNormalStack(), dRtr, null, true, stack.getItem(), null, true, false);
            boolean deferSend = false;
            if (reply != null) {// some pipes are not aware of the space in the adjacent inventory, so they return null
                if (reply.maxNumberOfItems < wanted) {
                    wanted = reply.maxNumberOfItems;
                    if (wanted <= 0) {
                        this.service.getItemOrderManager().deferSend();
                        return 0;
                    }
                    deferSend = true;
                }
            }
            if (!this.service.canUseEnergy(wanted * neededEnergy())) {
                return -1;
            }
            ItemStack removed = invUtil.getMultipleItems(item, wanted);
            if (removed.isEmpty()) {
                continue;
            }
            int sent = removed.getCount();
            this.service.useEnergy(sent * neededEnergy());

            final IRoutedItem routedItem = this.service
                .sendStack(removed, destination, itemSendMode(), info, neighbor.getDirection());
            this.service.getItemOrderManager().sendSuccessfull(sent, deferSend, routedItem);
            return sent;
        }

        this.service.getItemOrderManager().sendFailed();
        return 0;
    }

    public int getTotalItemCount(final ItemIdentifier item) {
        if (filterBlocksItem(item)) {
            return 0;
        }
        return inventoriesWithMode().map(invUtil -> invUtil.itemCount(item)).reduce(Integer::sum).orElse(0);
    }

    /*** GUI STUFF ***/

    public IItemIdentifierInventory getFilterInventory() {
        return filterInventory;
    }

    @Override
    public List<String> getClientInformation() {
        List<String> list = new ArrayList<>();
        list.add(!(boolean) isExclusionFilter.getValue() ? "Included" : "Excluded");
        list.add("Mode: " + providerMode.getValue().name());
        list.add("<inventory>");
        list.add("<that>" + filterInventory.getTagKey());
        return list;
    }

    private void checkUpdate(@Nullable Player player) {
        if (localModeWatchers.isEmpty() && player == null) {
            return;
        }
        displayList.clear();
        displayMap.clear();
        getAllItems(displayMap, new ArrayList<>(0));
        displayList.ensureCapacity(displayMap.size());
        displayList.addAll(displayMap.entrySet().stream()
            .map(item -> new ItemIdentifierStack(item.getKey(), item.getValue()))
            .toList());
        if (!oldList.equals(displayList)) {
            oldList.clear();
            oldList.ensureCapacity(displayList.size());
            oldList.addAll(displayList);
            localModeWatchers.send(new ModuleInventoryMessage(ModuleTarget.of(this), List.copyOf(displayList)));
        } else if (player != null) {
            if (player instanceof ServerPlayer inventoryWatcher) {
                PacketDistributor.sendToPlayer(inventoryWatcher,
                    new ModuleInventoryMessage(ModuleTarget.of(this), List.copyOf(displayList)));
            }
        }
    }

    @Override
    public void startWatching(Player player) {
        localModeWatchers.add(player);
        checkUpdate(player);
    }

    @Override
    public void stopWatching(Player player) {
        localModeWatchers.remove(player);
    }

    @Override
    public @Nullable IHUDModuleRenderer getHUDRenderer() {
        return HUD;
    }

    @Override
    public void handleInvContent(Collection<@Nullable ItemIdentifierStack> list) {
        displayList.clear();
        displayList.addAll(list);
    }

    @Override
    public boolean hasGenericInterests() {
        return false;
    }

    @Override
    public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
        //when filter is empty or in exclude mode, this is interested in attached inventory already
        if (!isExclusionFilter.getValue() && !filterInventory.isEmpty()) {
            // when items included this is only interested in items in the filter
            itemIdentifiers.addAll(filterInventory.getItemsAndCount().keySet());
        }
        // A request only reaches a crafter that is interested in the item.
        itemIdentifiers.addAll(craftableItems());
    }

    @Override
    public boolean interestedInAttachedInventory() {
        // when items included this is only interested in items in the filter
        return isExclusionFilter.getValue() || filterInventory.isEmpty();
        // when items not included, we can only serve those items in the filter.
    }

    @Override
    public boolean interestedInUndamagedID() {
        return false;
    }

    @Override
    public boolean receivePassive() {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
        return new ProviderMenu(containerId, inventory, target, this);
    }

    @Override
    public void writeMenuData(RegistryFriendlyByteBuf buffer) {
        TagValueOutput moduleOutput = TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING, buffer.registryAccess());
        serialize(moduleOutput);
        buffer.writeNbt(moduleOutput.buildResult());
    }

    private @Nullable IInventoryUtil getInventoryUtilWithMode(NeighborBlockEntity<BlockEntity> neighbor) {
        return InventoryUtilFactory.get()
            .getHidingInventoryUtil(neighbor.getBlockEntity(), neighbor.getOurDirection(), providerMode.getValue());
    }


    /**
     * Crafting by the system behind the inventory. It brings its own ingredients, so the network is
     * asked for none, but it says how many it can make and what it lacks for the rest.
     */
    private static final class ExternalCraftingTemplate extends ItemCraftingTemplate {

        private final IExternalCrafter crafter;
        /**
         * Calculations already made for this request, by amount: the request tree asks the same
         * question more than once while it balances crafters.
         */
        private final Map<Integer, IExternalCrafter.Calculation> calculations = new TreeMap<>();
        private Map<ItemIdentifier, Integer> lastMissing = Map.of();

        private ExternalCraftingTemplate(ItemIdentifier item, IExternalCrafter crafter, ModuleProvider provider) {
            super(new ItemIdentifierStack(item, 1), provider, 0);
            this.crafter = crafter;
        }

        @Override
        public int getMaxCraftingSets(int wanted) {
            IExternalCrafter.Calculation calculation = calculations.computeIfAbsent(wanted,
                amount -> crafter.calculate(result.getItem(), amount));
            if (calculation.craftable() < wanted) {
                lastMissing = calculation.missing();
            }
            return calculation.craftable();
        }

        @Override
        public List<IResource> getMissingFor(int sets) {
            // What the system said it lacks for the amount it was last short of, as a terminal says.
            return lastMissing.entrySet().stream()
                .map(entry -> (IResource) new ItemResource(new ItemIdentifierStack(entry.getKey(), entry.getValue()),
                    null))
                .toList();
        }
    }
}
