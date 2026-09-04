package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDStringBasedItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.StringBasedItemSinkListMessage;
import logisticspipes.network.to_server.module.SetStringBasedItemSinkListMessage;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.inventory.ModuleAnalysisMenu;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.StringListProperty;

public class ModuleModBasedItemSink extends LogisticsModule
		implements IStringBasedModule, IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, IModuleMenuProvider {

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
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer,
					new StringBasedItemSinkListMessage(ModuleTarget.of(this), List.copyOf(modList)));
		}
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	@Override
	public void listChanged() {
		final IWorldProvider worldProvider = this.worldProvider;
		if (worldProvider == null) return;
		final List<String> names = List.copyOf(modList);
		if (worldProvider.getWorld() instanceof ServerLevel) {
			localModeWatchers.send(new StringBasedItemSinkListMessage(ModuleTarget.of(this), names));
		} else {
			ClientPacketDistributor.sendToServer(
					new SetStringBasedItemSinkListMessage(ModuleTarget.of(this), names));
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
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
		return new ModuleAnalysisMenu(LPMenuTypes.STRING_BASED_ITEM_SINK.get(), containerId, inventory, target, this);
	}

	@Override
	public void writeMenuData(RegistryFriendlyByteBuf buffer) {
		TagValueOutput moduleOutput = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, buffer.registryAccess());
		serialize(moduleOutput);
		buffer.writeNbt(moduleOutput.buildResult());
	}

}
