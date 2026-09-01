/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.interfaces.IWatchingHandler.WatchMode;
import logisticspipes.gui.hud.HUDProvider;
import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.IOrderManagerContentReceiver;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.to_server.PipeHudWatchMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeItemsProviderLogistics extends CoreRoutedPipe implements IProvideItems, IHeadUpDisplayRendererProvider, IChestContentReceiver, IOrderManagerContentReceiver {

	public final LinkedList<ItemIdentifierStack> itemListOrderer = new LinkedList<>();
	private final HUDProvider HUD = new HUDProvider(this);

	protected final ModuleProvider providerModule = new ModuleProvider();

	public ArrayList<ItemIdentifierStack> getDisplayList() {
		return providerModule.displayList;
	}

	public PipeItemsProviderLogistics(Item item) {
		super(item);
		providerModule.registerHandler(this, this);
		providerModule.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public void onAllowedRemoval() {
		providerModule.onBlockRemoval();
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_PROVIDER_TEXTURE;
	}

	@Override
	public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
		if (!isEnabled()) {
			return;
		}
		providerModule.canProvide(tree, root, filters);
	}

	@Override
	public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info) {
		return providerModule.fullFill(promise, destination, info);
	}

	@Override
	public void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters) {
		if (!isEnabled()) {
			return;
		}
		providerModule.getAllItems(items, filters);
	}

	@Override
	public ModuleProvider getLogisticsModule() {
		return providerModule;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return providerModule.itemSendMode();
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
			providerModule.startWatching(player);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(Player player, WatchMode mode) {
		if (mode == WatchMode.HUD) {
			providerModule.stopWatching(player);
		} else {
			super.playerStopWatching(player, mode);
		}
	}

	@Override
	public void setReceivedChestContent(Collection<ItemIdentifierStack> list) {
		providerModule.displayList.clear();
		providerModule.displayList.ensureCapacity(list.size());
		providerModule.displayList.addAll(list);
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public void setOrderManagerContent(Collection<ItemIdentifierStack> list) {
		itemListOrderer.clear();
		itemListOrderer.addAll(list);
	}

	@Override
	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
		if (providerModule.isExclusionFilter.getValue() || providerModule.filterInventory.isEmpty()) {
			itemIdentifiers.addAll(
					providerModule.inventoriesWithMode()
							.flatMap(invUtil -> invUtil.getItems().stream())
							.filter(item -> !providerModule.filterBlocksItem(item))
							.collect(Collectors.toList()));
		} else {
			providerModule.collectSpecificInterests(itemIdentifiers);
		}
	}

	@Override
	public double getLoadFactor() {
		return (orderItemManager.totalAmountCountInAllOrders() + 63) / 64.0;
	}
}
