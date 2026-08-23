package logisticspipes;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.PlayerConfigToClientPacket;
import logisticspipes.network.packets.chassis.ChestGuiClosed;
import logisticspipes.network.packets.chassis.ChestGuiOpened;
import logisticspipes.network.packets.gui.GuiReopenPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.GuiOverlay;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.string.ChatColor;
import logisticspipes.world.item.LPItems;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.items.IItemHandler;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.PlayerConfiguration;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsEventListener {

	public static final WeakHashMap<Player, List<WeakReference<AsyncQuicksortModule>>> chestQuickSortConnection = new WeakHashMap<>();
	public static Map<ChunkPos, PlayerCollectionList> watcherList = new ConcurrentHashMap<>();

	@SubscribeEvent
	public void onEntitySpawn(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof ItemEntity itemEntity && !event.getLevel().isClientSide) {
			ItemStack stack = itemEntity.getItem();
			if (!stack.isEmpty() &&
					stack.getItem() instanceof IItemAdvancedExistance itemAdvancedExistence &&
					!itemAdvancedExistence.canExistInWorld(stack)) {
				event.setCanceled(true);
				return;
			}
			CompoundTag tag = stack.getOrDefault(
					DataComponents.CUSTOM_DATA,
					CustomData.EMPTY
			).copyTag();
			for (String key : tag.keySet()) {
				if (key.startsWith("logisticspipes:routingdata")) {
					ItemRoutingInformation info =
							ItemRoutingInformation.restoreFromNBT(TagValueInput.create(ProblemReporter.DISCARDING,
									event.getLevel().registryAccess(), tag.getCompoundOrEmpty(key)));

					info.setItemTimedout();

					itemEntity.setItem(
							info.getItem()
									.getItem()
									.makeNormalStack(stack.getCount())
					);
					break;
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
		if (MainProxy.isServer(event.getEntity().level())) {
			final BlockEntity tile = event.getEntity().level().getBlockEntity(event.getPos());
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
					if (!((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).canBeDestroyedByPlayer(event.getEntity())) {
						event.setCanceled(true);
						event.getEntity().displayClientMessage(Component.translatable("lp.chat.permissiondenied"), false);
						((LogisticsTileGenericPipe) tile).scheduleNeighborChange();
						Level level = event.getEntity().level();
						BlockPos pos = tile.getBlockPos();
						BlockState state = level.getBlockState(pos);
						level.sendBlockUpdated(pos, state, state, 2);
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).delayTo = System.currentTimeMillis() + 200;
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).repeatFor = 10;
					} else {
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).setDestroyByPlayer();
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
		if (MainProxy.isClient(event.getEntity().level())) return;
		Level level = event.getEntity().level();
		BlockPos pos = event.getPos();
		BlockEntity te = level.getBlockEntity(pos);
		if (te == null) return;
		// Only act on blocks that expose an item handler (chests, barrels, etc.)
		IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (itemHandler == null) return;

		Player player = event.getEntity();
		List<WeakReference<AsyncQuicksortModule>> modules = null;

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockEntity neighbor = level.getBlockEntity(neighborPos);
			if (!(neighbor instanceof LogisticsTileGenericPipe)) continue;
			LogisticsTileGenericPipe pipeTile = (LogisticsTileGenericPipe) neighbor;
			if (!(pipeTile.pipe instanceof PipeLogisticsChassis)) continue;
			PipeLogisticsChassis chassis = (PipeLogisticsChassis) pipeTile.pipe;
			// The chassis must be pointing at the clicked block
			if (chassis.getPointedOrientation() != dir.getOpposite()) continue;

			final List<WeakReference<AsyncQuicksortModule>> found = modules == null ? new ArrayList<>() : modules;
			chassis.getModules().getModules()
					.filter(m -> m instanceof AsyncQuicksortModule)
					.forEach(m -> found.add(new WeakReference<>((AsyncQuicksortModule) m)));
			if (!found.isEmpty()) modules = found;
		}

		if (modules != null && !modules.isEmpty()) {
			chestQuickSortConnection.put(player, modules);
		}
	}

	public static HashMap<ResourceKey<Level>, Long> WorldLoadTime = new HashMap<>();

	@SubscribeEvent
	public void WorldLoad(LevelEvent.Load event) {
		if (MainProxy.isServer(event.getLevel())) {
			if (event.getLevel() instanceof Level level) {
				ResourceKey<Level> dim = level.dimension();
				if (!LogisticsEventListener.WorldLoadTime.containsKey(dim)) {
					LogisticsEventListener.WorldLoadTime.put(dim, System.currentTimeMillis());
				}
			}
		}
		if (MainProxy.isClient(event.getLevel())) {
			SimpleServiceLocator.routerManager.clearClientRouters();
			LogisticsHUDRenderer.instance().clear();
		}
	}

	@SubscribeEvent
	public void WorldUnload(LevelEvent.Unload event) {
		if (MainProxy.isServer(event.getLevel())) {
			if (event.getLevel() instanceof Level level) {
				SimpleServiceLocator.routerManager.dimensionUnloaded(level.dimension().location());
			}
		}
	}

	@SubscribeEvent
	public void watchChunk(ChunkWatchEvent.Watch event) {
		ChunkPos pos = event.getPos();
		if (!LogisticsEventListener.watcherList.containsKey(pos)) {
			LogisticsEventListener.watcherList.put(pos, new PlayerCollectionList());
		}
		LogisticsEventListener.watcherList.get(pos).add(event.getPlayer());
	}

	@SubscribeEvent
	public void unWatchChunk(ChunkWatchEvent.UnWatch event) {
		ChunkPos pos = event.getPos();
		if (LogisticsEventListener.watcherList.containsKey(pos)) {
			LogisticsEventListener.watcherList.get(pos).remove(event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (MainProxy.isServer(event.getEntity().level())) {
			SimpleServiceLocator.securityStationManager.sendClientAuthorizationList(event.getEntity());
		}

		SimpleServiceLocator.serverBufferHandler.clear(event.getEntity());
		ClientConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(PlayerIdentifier.get(event.getEntity()));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PlayerConfigToClientPacket.class).setConfig(config), event.getEntity());
	}

	@SubscribeEvent
	public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		SimpleServiceLocator.serverBufferHandler.clear(event.getEntity());
	}

	@AllArgsConstructor
	private static class GuiEntry {

		@Getter
		private final int xCoord;
		@Getter
		private final int yCoord;
		@Getter
		private final int zCoord;
		@Getter
		private final int guiID;
		@Getter
		@Setter
		private boolean isActive;
	}

	@Getter(lazy = true)
	private static final Queue<GuiEntry> guiPos = new LinkedList<>();

	// Handle GuiReopen — Opening event (screen becoming visible)
	@SubscribeEvent
	public void onGuiOpen(ScreenEvent.Opening event) {
		// Guard: no server connection (e.g. main menu) — nothing to notify
		if (Minecraft.getInstance().getConnection() == null) {
			return;
		}
		if (!LogisticsEventListener.getGuiPos().isEmpty()) {
			GuiEntry part = LogisticsEventListener.getGuiPos().peek();
			part.setActive(true);
		}
		if (event.getScreen() instanceof AbstractContainerScreen) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiOpened.class));
		} else {
			QuickSortChestMarkerStorage.getInstance().disable();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiClosed.class));
		}
	}

	// Handle GuiReopen — Closing event (screen being dismissed without a replacement)
	@SubscribeEvent
	public void onGuiClose(ScreenEvent.Closing event) {
		// Guard: no server connection (e.g. main menu) — nothing to notify
		if (Minecraft.getInstance().getConnection() == null) {
			return;
		}
		if (!LogisticsEventListener.getGuiPos().isEmpty()) {
			GuiEntry part = LogisticsEventListener.getGuiPos().peek();
			if (part.isActive()) {
				part = LogisticsEventListener.getGuiPos().poll();
				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiReopenPacket.class).setGuiID(part.getGuiID()).setPosX(part.getXCoord()).setPosY(part.getYCoord()).setPosZ(part.getZCoord()));
				GuiOverlay.getInstance().setOverlaySlotActive(false);
			}
		}
		GuiOverlay.getInstance().setOverlaySlotActive(false);
	}

	public static void addGuiToReopen(int xCoord, int yCoord, int zCoord, int guiID) {
		LogisticsEventListener.getGuiPos().add(new GuiEntry(xCoord, yCoord, zCoord, guiID, false));
	}

	@SubscribeEvent
	public void clientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
		SimpleServiceLocator.clientBufferHandler.clear();
	}

	@SubscribeEvent
	public void onItemStackToolTip(ItemTooltipEvent event) {
		CompoundTag tag = event.getItemStack()
				.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag();

		for (String key : tag.keySet()) {
			if (key.startsWith("logisticspipes:routingdata")) {
				ItemRoutingInformation info =
						ItemRoutingInformation.restoreFromNBT(TagValueInput.create(ProblemReporter.DISCARDING,
							event.getEntity().registryAccess(), tag.getCompoundOrEmpty(key)));
				List<Component> list = event.getToolTip();
				list.set(0, Component.literal(ChatColor.RED + "!!! " + ChatColor.WHITE)
						.append(list.get(0))
						.append(Component.literal(ChatColor.RED + " !!!" + ChatColor.WHITE)));
				list.add(1, Component.translatable("itemstackinfo.lprouteditem"));
				list.add(2, Component.translatable("itemstackinfo.lproutediteminfo"));
				list.add(3, Component.literal(TextUtil.translate("itemstackinfo.lprouteditemtype") + ": " + info.getItem().toString()));
			}
		}
	}

	/**
	 * Replaces the 1.12.2 ASM injection of TEControl.validate/invalidate into all TileEntity subclasses.
	 * When any block changes (placed, broken, or neighbour update), check the six adjacent positions for
	 * LP routing pipes and flag their routers for adjacency recheck.  This covers the case where a
	 * non-LP inventory (chest, machine, etc.) is placed or removed next to a provider/requester pipe.
	 */
	@SubscribeEvent
	public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
		LevelAccessor levelAccessor = event.getLevel();
		if (!(levelAccessor instanceof Level)) return;
		Level level = (Level) levelAccessor;
		if (level.isClientSide()) return;
		BlockPos changed = event.getPos();
		for (Direction dir : event.getNotifiedSides()) {
			BlockEntity neighbor = level.getBlockEntity(changed.relative(dir));
			if (neighbor instanceof LogisticsTileGenericPipe pipe
					&& pipe.pipe instanceof CoreRoutedPipe routedPipe
					&& !routedPipe.stillNeedReplace()) {
				routedPipe.getRouter().update(false, routedPipe);
			}
		}
	}

	@SubscribeEvent
	public void onItemCrafting(PlayerEvent.ItemCraftedEvent event) {
		if (!event.getEntity().level().isClientSide && !event.getCrafting().isEmpty()) {
			if (BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).getNamespace().equals(LPConstants.ID)) {
				PlayerIdentifier identifier = PlayerIdentifier.get(event.getEntity());
				PlayerConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(identifier);
				if (!config.getHasCraftedLPItem() && !LogisticsPipes.isDEBUG()) {
					ItemStack book = LPItems.GUIDE_BOOK.toStack(1);
					event.getEntity().addItem(book);

					config.setHasCraftedLPItem(true);
					LogisticsPipes.getServerConfigManager().setPlayerConfiguration(identifier, config);
				}
			}
		}
	}
}
