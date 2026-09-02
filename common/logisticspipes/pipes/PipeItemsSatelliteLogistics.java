/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.HUDSatellite;
import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleSatellite;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.packets.hud.ChestContent;
import logisticspipes.network.to_client.pipe.SatelliteNameMessage;
import logisticspipes.network.to_server.pipe.PipeHudWatchMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;

public class PipeItemsSatelliteLogistics extends CoreRoutedPipe implements IRequestItems, IRequireReliableTransport, IHeadUpDisplayRendererProvider, IChestContentReceiver, SatellitePipe {

	public static final Set<PipeItemsSatelliteLogistics> AllSatellites = Collections.newSetFromMap(new WeakHashMap<>());

	// called only on server shutdown
	public static void cleanup() {
		PipeItemsSatelliteLogistics.AllSatellites.clear();
	}

	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final LinkedList<ItemIdentifierStack> itemList = new LinkedList<>();
	private final HUDSatellite HUD = new HUDSatellite(this);
	protected final LinkedList<ItemIdentifierStack> lostItems = new LinkedList<>();
	private final ModuleSatellite moduleSatellite;

	@Getter
	private String satellitePipeName = "";

	public PipeItemsSatelliteLogistics(Item item) {
		super(item);
		throttleTime = 40;
		moduleSatellite = new ModuleSatellite();
		moduleSatellite.registerHandler(this, this);
		moduleSatellite.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_SATELLITE_TEXTURE;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (isNthTick(20) && localModeWatchers.size() > 0) {
			updateInv(false);
		}
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return moduleSatellite;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void startWatching() {
		ClientPacketDistributor.sendToServer(new PipeHudWatchMessage(getPos(), true));
	}

	@Override
	public void stopWatching() {
		ClientPacketDistributor.sendToServer(new PipeHudWatchMessage(getPos(), false));
	}

	private void addToList(ItemIdentifierStack stack) {
		for (ItemIdentifierStack ident : itemList) {
			if (ident.getItem().equals(stack.getItem())) {
				ident.setStackSize(ident.getStackSize() + stack.getStackSize());
				return;
			}
		}
		itemList.addLast(stack);
	}

	private void updateInv(boolean force) {
		ArrayList<ItemIdentifierStack> oldList = new ArrayList<>(itemList);
		itemList.clear();
		itemList.addAll(
				getAvailableAdjacent().inventories().stream()
						.map(LPNeighborTileEntityKt::getInventoryUtil)
						.filter(Objects::nonNull)
						.flatMap(invUtil -> invUtil.getItemsAndCount().entrySet().stream().map(itemIdentifierAndCount -> new ItemIdentifierStack(itemIdentifierAndCount.getKey(), itemIdentifierAndCount.getValue())))
						.collect(Collectors.toList())
		);
		if (!oldList.equals(itemList) || force) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ChestContent.class).setIdentList(itemList).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
		}
	}

	@Override
	public void playerStartWatching(Player player, WatchMode mode) {
		if (mode == WatchMode.HUD) {
			localModeWatchers.add(player);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer,
						new SatelliteNameMessage(getPos(), satellitePipeName));
			}
			updateInv(true);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(Player player, WatchMode mode) {
		super.playerStopWatching(player, mode);
		localModeWatchers.remove(player);
	}

	@Override
	public void setReceivedChestContent(Collection<ItemIdentifierStack> list) {
		itemList.clear();
		itemList.addAll(list);
	}

	@Override
	public Level getLevelForHUD() {
		return getWorld();
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
        satellitePipeName = input.getInt("satelliteid")
            .map(integer -> Integer.toString(integer))
            .orElseGet(() -> input.getStringOr("satellitePipeName", ""));
		if (MainProxy.isServer(getWorld())) {
			ensureAllSatelliteStatus();
		}
	}

	@Override
	public void serialize(ValueOutput output) {
		output.putString("satellitePipeName", satellitePipeName);
		super.serialize(output);
	}

	public void ensureAllSatelliteStatus() {
		if (satellitePipeName.isEmpty()) {
			PipeItemsSatelliteLogistics.AllSatellites.remove(this);
		}
		if (!satellitePipeName.isEmpty()) {
			PipeItemsSatelliteLogistics.AllSatellites.add(this);
		}
	}

	public void updateWatchers() {
		final SatelliteNameMessage message = new SatelliteNameMessage(getPos(), satellitePipeName);
		localModeWatchers.send(message);
		TargetLookup.sendToChunkWatchers(getContainer(), message);
	}

	@Override
	public void onAllowedRemoval() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		PipeItemsSatelliteLogistics.AllSatellites.remove(this);
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		// Send the satellite id when opening gui
		if (entityplayer instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer,
					new SatelliteNameMessage(getPos(), satellitePipeName));
		}
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.SatelliteGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}

	@Override
	public void throttledUpdateEntity() {
		super.throttledUpdateEntity();
		if (lostItems.isEmpty()) {
			return;
		}
		final Iterator<ItemIdentifierStack> iterator = lostItems.iterator();
		while (iterator.hasNext()) {
			ItemIdentifierStack stack = iterator.next();
			int received = RequestTree.requestPartial(stack, (CoreRoutedPipe) container.pipe, null);
			if (received > 0) {
				if (received == stack.getStackSize()) {
					iterator.remove();
				} else {
					stack.setStackSize(stack.getStackSize() - received);
				}
			}
		}
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		lostItems.add(item);
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
	}

	@Override
	public Set<SatellitePipe> getSatellitesOfType() {
		return Collections.unmodifiableSet(AllSatellites);
	}

	@Override
	public void setSatellitePipeName(String satellitePipeName) {
		this.satellitePipeName = satellitePipeName;
	}

	@Override
	public List<ItemIdentifierStack> getItemList() {
		return itemList;
	}
}
