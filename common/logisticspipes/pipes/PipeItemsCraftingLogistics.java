/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

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

import logisticspipes.gui.hud.HUDCrafting;
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
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCQueued;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IPromise;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.IHavePriority;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

@CCType(name = "LogisticsPipes:Crafting")
public class PipeItemsCraftingLogistics extends CoreRoutedPipe implements ICraftItems, IRequireReliableTransport, IHeadUpDisplayRendererProvider, IChangeListener, IOrderManagerContentReceiver, IHavePriority {

	protected final ModuleCrafter craftingModule;

	public final LinkedList<ItemIdentifierStack> oldList = new LinkedList<>();
	public final LinkedList<ItemIdentifierStack> displayList = new LinkedList<>();
	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final HUDCrafting HUD = new HUDCrafting(this);

	private boolean doContentUpdate = true;

	public PipeItemsCraftingLogistics(Item item) {
		super(item);
		craftingModule = new ModuleCrafter();
		craftingModule.registerHandler(this, this);
		craftingModule.registerPosition(ModulePositionType.IN_PIPE, 0);
		throttleTime = 40;
		orderItemManager = new LogisticsItemOrderManager(this, this); // null by default when not needed
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
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_CRAFTER_TEXTURE;
	}

	@Override
	public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
		if (!isEnabled()) {
			return;
		}
		craftingModule.canProvide(tree, root, filters);

	}

	@Override
	public ICraftingTemplate addCrafting(IResource toCraft) {

		if (!isEnabled()) {
			return null;
		}

		return craftingModule.addCrafting(toCraft);
	}

	@Override
	public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info) {
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
	@Deprecated
	public List<ItemIdentifierStack> getCraftedItems() {
		return craftingModule.getCraftedItems();
	}

	@Override
	public ModuleCrafter getLogisticsModule() {
		return craftingModule;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public int getTodo() {
		return Objects.requireNonNull(orderItemManager).totalAmountCountInAllOrders();
	}

	@Override
	public Level getLevelForHUD() {
		return getWorld();
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
				PacketDistributor.sendToPlayer(serverPlayer, new OrderManagerContentMessage(getPos(), List.copyOf(oldList)));
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
		LinkedList<ItemIdentifierStack> all = Objects.requireNonNull(orderItemManager).getContentList(getWorld());
		if (!oldList.equals(all)) {
			oldList.clear();
			oldList.addAll(all);
			localModeWatchers.send(new OrderManagerContentMessage(getPos(), List.copyOf(all)));
		}
	}

	@Override
	public void setOrderManagerContent(Collection<ItemIdentifierStack> list) {
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
	@CCCommand(description = "Imports the crafting recipe from the connected machine/crafter")
	@CCQueued()
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
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		craftingModule.itemArrived(item, info);
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
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
