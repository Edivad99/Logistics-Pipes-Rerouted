package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.gui.hud.modules.HUDStringBasedItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.StringBasedItemSinkModuleGuiInHand;
import logisticspipes.network.guis.module.inpipe.StringBasedItemSinkModuleGuiSlot;
import logisticspipes.network.packets.module.ModuleBasedItemSinkList;
import logisticspipes.network.to_server.ModuleWatchMessage;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.StringListProperty;

public class ModuleModBasedItemSink extends LogisticsModule
		implements IStringBasedModule, IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, Gui {

	public final StringListProperty modList = new StringListProperty("");

	private final IHUDModuleRenderer HUD = new HUDStringBasedItemSink(this);

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;

	public static String getName() {
		return "item_sink_mod";
	}

	@Override
	public String getLPName() {
		return getName();
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.singletonList(modList);
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.ModBasedItemSink, 0, true, false, 5, 0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
		if (bestPriority > reply.fixedPriority.ordinal() || (bestPriority == reply.fixedPriority.ordinal()
				&& bestCustomPriority >= reply.customPriority)) {
			return null;
		}
		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		if (modList.contains(item.getModName())) {
			if (service.canUseEnergy(5)) {
				return reply;
			}
		}
		return null;
	}


	@Override
	public void tick() {}

	@Override
	public List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Mods: ");
		list.addAll(modList);
		return list;
	}



	@Override
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		TagValueOutput moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
		serialize(moduleOutput);
		CompoundTag nbt = moduleOutput.buildResult();
		MainProxy.sendPacketToPlayer(
				PacketHandler.getPacket(ModuleBasedItemSinkList.class).setNbt(nbt).setModulePos(this), player);
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	@Override
	public void listChanged() {
		final IWorldProvider worldProvider = this.worldProvider;
		if (worldProvider == null) return;
		if (MainProxy.isServer(worldProvider.getWorld())) {
			TagValueOutput moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, worldProvider.getWorld().registryAccess());
			serialize(moduleOutput);
			CompoundTag nbt = moduleOutput.buildResult();
			MainProxy.sendToPlayerList(
					PacketHandler.getPacket(ModuleBasedItemSinkList.class).setNbt(nbt).setModulePos(this),
					localModeWatchers);
		} else {
			TagValueOutput moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, worldProvider.getWorld().registryAccess());
			serialize(moduleOutput);
			CompoundTag nbt = moduleOutput.buildResult();
			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(ModuleBasedItemSinkList.class).setNbt(nbt).setModulePos(this));
		}
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return HUD;
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return false;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

	@Override
	public StringListProperty stringListProperty() {
		return modList;
	}

	@Override
	public String getStringForItem(ItemIdentifier ident) {
		return ident.getModName();
	}

	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		TagValueOutput moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, getWorld().registryAccess());
		serialize(moduleOutput);
		CompoundTag nbt = moduleOutput.buildResult();
		return NewGuiHandler.getGui(StringBasedItemSinkModuleGuiSlot.class).setNbt(nbt);
	}

	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(StringBasedItemSinkModuleGuiInHand.class);
	}

}
