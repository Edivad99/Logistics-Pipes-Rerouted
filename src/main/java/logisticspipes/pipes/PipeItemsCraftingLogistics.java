package logisticspipes.pipes;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.renderer.hud.HUDCrafting;
import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.IOrderManagerContentReceiver;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.to_client.orderer.OrderManagerContentMessage;
import logisticspipes.network.to_server.pipe.PipeHudWatchMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.order.ItemOrderManager;
import logisticspipes.request.order.Order;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.request.resources.IResource;
import logisticspipes.textures.PipeTexture;
import logisticspipes.utils.IHavePriority;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeItemsCraftingLogistics extends CoreRoutedPipe
    implements ICraftItems, IRequireReliableTransport, IHeadUpDisplayRendererProvider, IChangeListener,
    IOrderManagerContentReceiver, IHavePriority {

    public final LinkedList<@Nullable ItemIdentifierStack> displayList = new LinkedList<>();
    protected final ModuleCrafter craftingModule;
    private final LinkedList<ItemIdentifierStack> oldList = new LinkedList<>();
    private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
    private final HUDCrafting HUD = new HUDCrafting(this);

    private boolean doContentUpdate = true;

    public PipeItemsCraftingLogistics(Item item) {
        super(item);
        craftingModule = new ModuleCrafter();
        craftingModule.registerHandler(this, this);
        craftingModule.registerPosition(ModulePositionType.IN_PIPE, 0);
        throttleTime = 40;
        orderItemManager = new ItemOrderManager(this, this); // null by default when not needed
    }

    @Override
    public void onAllowedRemoval() {
        Preconditions.checkNotNull(orderItemManager);
        while (orderItemManager.hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
            orderItemManager.sendFailed();
        }
    }

    @Override
    public void enabledUpdateEntity() {
        super.enabledUpdateEntity();
        if (doContentUpdate) {
            checkContentUpdate();
        }
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.CRAFTING;
    }

    @Override
    public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
        if (!isEnabled()) {
            return;
        }
        craftingModule.canProvide(tree, root, filters);

    }

    @Override
    public @Nullable ICraftingTemplate addCrafting(IResource toCraft) {

        if (!isEnabled()) {
            return null;
        }

        return craftingModule.addCrafting(toCraft);
    }

    @Override
    public @Nullable Order fullFill(ItemPromise promise, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info) {
        return craftingModule.fullFill(promise, destination, info);
    }

    @Override
    public void registerExtras(IPromise promise) {
        craftingModule.registerExtras(promise);
    }

    @Override
    public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filters) {
        craftingModule.getAllItems(list, filters);
    }

    @Override
    public boolean canCraft(IResource toCraft) {
        return craftingModule.canCraft(toCraft);
    }

    @Override
    public List<ItemIdentifierStack> getCraftedItems() {
        return craftingModule.getCraftedItems();
    }

    @Override
    public ModuleCrafter getLogisticsModule() {
        return craftingModule;
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
    }

    @Override
    public int getTodo() {
        return Objects.requireNonNull(orderItemManager).totalAmountCountInAllOrders();
    }

    @Override
    public @Nullable Level getLevelForHUD() {
        return getLevel();
    }

    @Override
    public void startWatching() {
        ClientPacketDistributor.sendToServer(new PipeHudWatchMessage(getPos(), true));
    }

    @Override
    public void stopWatching() {
        ClientPacketDistributor.sendToServer(new PipeHudWatchMessage(getPos(), false));
    }

    @Override
    public void playerStartWatching(Player player, WatchMode mode) {
        if (mode == WatchMode.HUD) {
            localModeWatchers.add(player);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer,
                    new OrderManagerContentMessage(getPos(), List.copyOf(oldList)));
            }
            craftingModule.startWatching(player);
        } else {
            super.playerStartWatching(player, mode);
        }
    }

    @Override
    public void playerStopWatching(Player player, WatchMode mode) {
        super.playerStopWatching(player, mode);
        localModeWatchers.remove(player);
        craftingModule.stopWatching(player);
    }

    @Override
    public void listenedChanged() {
        doContentUpdate = true;
    }

    private void checkContentUpdate() {
        doContentUpdate = false;
        LinkedList<ItemIdentifierStack> all = Objects.requireNonNull(orderItemManager).getContentList(getLevel());
        if (!oldList.equals(all)) {
            oldList.clear();
            oldList.addAll(all);
            localModeWatchers.send(new OrderManagerContentMessage(getPos(), List.copyOf(all)));
        }
    }

    @Override
    public void setOrderManagerContent(Collection<@Nullable ItemIdentifierStack> list) {
        displayList.clear();
        displayList.addAll(list);
    }

    @Override
    public IHeadUpDisplayRenderer getRenderer() {
        return HUD;
    }

    @Override
    public double getLoadFactor() {
        return (Objects.requireNonNull(orderItemManager).totalAmountCountInAllOrders() + 63.0) / 64.0;
    }

    /* ComputerCraftCommands */
    public void reimport() {
        craftingModule.importFromCraftingTable(null);
    }

    @Override
    public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
        craftingModule.collectSpecificInterests(itemIdentifiers);
    }

    @Override
    public int getPriority() {
        return craftingModule.priority.getValue();
    }

    @Override
    public void itemArrived(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        craftingModule.itemArrived(item, info);
    }

    @Override
    public void itemLost(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        craftingModule.itemLost(item, info);
    }

    public Container getDummyInventory() {
        return craftingModule.dummyInventory;
    }

    public boolean hasCraftingSign() {
        for (int i = 0; i < 6; i++) {
            if (signItem[i] instanceof CraftingPipeSign) {
                return true;
            }
        }
        return false;
    }
}
