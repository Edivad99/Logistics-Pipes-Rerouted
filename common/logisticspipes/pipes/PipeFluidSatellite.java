package logisticspipes.pipes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

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
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleSatellite;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.ChestContentMessage;
import logisticspipes.network.to_client.pipe.SatelliteNameMessage;
import logisticspipes.network.to_server.pipe.PipeHudWatchMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeFluidSatellite extends FluidRoutedPipe implements IRequestFluid, IRequireReliableFluidTransport, IHeadUpDisplayRendererProvider, IChestContentReceiver, SatellitePipe {

	// from baseLogicLiquidSatellite
	public static final Set<PipeFluidSatellite> AllSatellites = new HashSet<>();

	// called only on server shutdown
	public static void cleanup() {
		PipeFluidSatellite.AllSatellites.clear();
	}

	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final List<ItemIdentifierStack> itemList = new LinkedList<>();
	private final List<ItemIdentifierStack> oldList = new LinkedList<>();
	private final HUDSatellite HUD = new HUDSatellite(this);
	protected final Map<FluidIdentifier, Integer> lostItems = new HashMap<>();
	private final ModuleSatellite moduleSatellite;

	@Getter
	private String satellitePipeName = "";

	public PipeFluidSatellite(Item item) {
		super(item);
		throttleTime = 40;
		moduleSatellite = new ModuleSatellite();
		moduleSatellite.registerHandler(this, this);
		moduleSatellite.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_SATELLITE;
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
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (isNthTick(20) && localModeWatchers.size() > 0) {
			updateInv(false);
		}
	}

	@Override
	public void sendFailed(FluidIdentifier liquid, Integer amount) {
		liquidLost(liquid, amount);
	}

	private void updateInv(boolean force) {
		itemList.clear();
		itemList.addAll(PipeFluidUtil.fluidsToItemList(this));

		if (!itemList.equals(oldList) || force) {
			oldList.clear();
			oldList.addAll(itemList);
			localModeWatchers.send(new ChestContentMessage(getPos(), List.copyOf(itemList)));
		}
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
			PipeFluidSatellite.AllSatellites.remove(this);
		} else {
			PipeFluidSatellite.AllSatellites.add(this);
		}
	}

	public void updateWatchers() {
		final LogisticsTileGenericPipe container = Objects.requireNonNull(getContainer());
		final SatelliteNameMessage message = new SatelliteNameMessage(getPos(), satellitePipeName);
		localModeWatchers.send(message);
		TargetLookup.sendToChunkWatchers(container, message);
	}

	@Override
	public void onAllowedRemoval() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		PipeFluidSatellite.AllSatellites.remove(this);
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
		final Iterator<Entry<FluidIdentifier, Integer>> iterator = lostItems.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<FluidIdentifier, Integer> stack = iterator.next();
			int received = RequestTree.requestFluidPartial(stack.getKey(), stack.getValue(), this, null);

			if (received > 0) {
				if (received == stack.getValue()) {
					iterator.remove();
				} else {
					stack.setValue(stack.getValue() - received);
				}
			}
		}
	}

	@Override
	public void liquidLost(FluidIdentifier item, int amount) {
		if (lostItems.containsKey(item)) {
			lostItems.put(item, lostItems.get(item) + amount);
		} else {
			lostItems.put(item, amount);
		}
	}

	@Override
	public void liquidArrived(FluidIdentifier item, int amount) {}

	@Override
	public void liquidNotInserted(FluidIdentifier item, int amount) {
		liquidLost(item, amount);
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
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
