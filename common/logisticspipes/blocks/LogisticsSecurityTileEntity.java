package logisticspipes.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.IGuiOpenController;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.SecurityStationGui;
import logisticspipes.network.packets.block.SecurityStationAutoDestroy;
import logisticspipes.network.packets.block.SecurityStationCC;
import logisticspipes.network.packets.block.SecurityStationCCIDs;
import logisticspipes.network.packets.block.SecurityStationId;
import logisticspipes.network.packets.block.SecurityStationOpenPlayer;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

public class LogisticsSecurityTileEntity extends LogisticsSolidBlockEntity implements IGuiOpenController, ISecurityProvider, IGuiTileEntity {

	public LogisticsSecurityTileEntity(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.SECURITY_STATION.get(), pos, state);
	}

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "ID Slots", 64);
	private PlayerCollectionList listener = new PlayerCollectionList();
    @Nullable
	private UUID secId = null;
	private Map<String, SecuritySettings> settingsList = new HashMap<>();
	public List<Integer> excludedCC = new ArrayList<>();
	public boolean allowCC = false;
	public boolean allowAutoDestroy = false;

	public static PlayerCollectionList byPassed = new PlayerCollectionList();
	public static final SecuritySettings allowAll = new SecuritySettings("");

	static {
		LogisticsSecurityTileEntity.allowAll.openGui = true;
		LogisticsSecurityTileEntity.allowAll.openRequest = true;
		LogisticsSecurityTileEntity.allowAll.openUpgrades = true;
		LogisticsSecurityTileEntity.allowAll.openNetworkMonitor = true;
		LogisticsSecurityTileEntity.allowAll.removePipes = true;
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.remove(this);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.add(this);
		}
	}

	// onChunkUnload removed in 1.20.1 — setRemoved() covers this case

	public void deauthorizeStation() {
		SimpleServiceLocator.securityStationManager.deauthorizeUUID(getSecId());
	}

	public void authorizeStation() {
		SimpleServiceLocator.securityStationManager.authorizeUUID(getSecId());
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCC.class).putInt(allowCC ? 1 : 0).setBlockPos(getBlockPos()), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationAutoDestroy.class).putInt(allowAutoDestroy ? 1 : 0).setBlockPos(getBlockPos()), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationId.class).setUuid(getSecId()).setBlockPos(getBlockPos()), player);
		SimpleServiceLocator.securityStationManager.sendClientAuthorizationList();
		listener.add(player);
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		listener.remove(player);
	}

    @Nullable
	public UUID getSecId() {
		if (MainProxy.isServer(getWorld())) {
			if (secId == null) {
				secId = UUID.randomUUID();
			}
		}
		return secId;
	}

	public void setClientUUID(UUID id) {
		if (MainProxy.isClient(getWorld())) {
			secId = id;
		}
	}

	public void setClientCC(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowCC = flag;
		}
	}

	public void setClientDestroy(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowAutoDestroy = flag;
		}
	}

	@Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
		input.getString("UUID").ifPresent(raw -> secId = UUID.fromString(raw));
		allowCC = input.getBooleanOr("allowCC", false);
		allowAutoDestroy = input.getBooleanOr("allowAutoDestroy", false);
		inv.deserialize(input);
		settingsList.clear();
		for (ValueInput entry : input.childrenListOrEmpty("settings")) {
			String name = entry.getStringOr("name", "");
			SecuritySettings settings = new SecuritySettings(name);
			settings.deserialize(entry.childOrEmpty("content"));
			settingsList.put(name, settings);
		}
		excludedCC.clear();
		for (int id : input.getIntArray("excludedCC").orElse(new int[0])) {
			excludedCC.add(id);
		}
	}

	@Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        UUID secId = getSecId();
        if (secId != null) {
            output.putString("UUID", secId.toString());
        }
        output.putBoolean("allowCC", allowCC);
        output.putBoolean("allowAutoDestroy", allowAutoDestroy);
		inv.serialize(output);
		ValueOutput.ValueOutputList list = output.childrenList("settings");
		for (Entry<String, SecuritySettings> entry : settingsList.entrySet()) {
			ValueOutput settingsEntry = list.addChild();
			settingsEntry.putString("name", entry.getKey());
			settingsEntry.putChild("content", entry.getValue());
		}
		output.putIntArray("excludedCC", excludedCC.stream().mapToInt(Integer::intValue).toArray());
	}

	public void buttonFreqCard(int integer, Player player) {
		switch (integer) {
			case 0: //--
				inv.setItem(0, ItemStack.EMPTY);
				break;
			case 1: //-
				inv.removeItem(0, 1);
				break;
			case 2: //+
				if (!useEnergy(10)) {
					player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
					return;
				}
				if (inv.getIDStackInSlot(0) == null) {
					ItemStack stack = new ItemStack(LPItems.ITEM_CARD.get(), 1);
					stack.set(LPDataComponents.UUID, getSecId());
					inv.setItem(0, stack);
				} else {
					ItemStack slot = inv.getItem(0);
					if (slot.getCount() < 64) {
						slot.grow(1);
						slot.set(LPDataComponents.UUID, getSecId());
						inv.setItem(0, slot);
					}
				}
				break;
			case 3: //++
				if (!useEnergy(640)) {
					player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
					return;
				}
				ItemStack stack = new ItemStack(LPItems.ITEM_CARD.get(), 64);
				stack.set(LPDataComponents.UUID, getSecId());
				inv.setItem(0, stack);
				break;
		}
	}

	public void handleOpenSecurityPlayer(Player player, String string) {
		SecuritySettings setting = settingsList.get(string);
		if (setting == null) {
			if (string.isEmpty()) return;
			setting = new SecuritySettings(string);
			settingsList.put(string, setting);
		}
		// The packet carries a raw CompoundTag while SecuritySettings serialises to a
		// ValueOutput; TagValueOutput is the adapter vanilla itself uses at that boundary.
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
			player.level().registryAccess());
		setting.serialize(output);
		MainProxy.sendPacketToPlayer(
			PacketHandler.getPacket(SecurityStationOpenPlayer.class).put(output.buildResult()), player);
	}

	public void saveNewSecuritySettings(CompoundTag tag, HolderLookup.Provider provider) {
		SecuritySettings setting = settingsList.get(tag.getStringOr("name", ""));
		if (setting == null) {
			setting = new SecuritySettings(tag.getStringOr("name", ""));
			settingsList.put(tag.getStringOr("name", ""), setting);
		}
		setting.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
	}

	public SecuritySettings getSecuritySettingsForPlayer(Player entityplayer, boolean usePower) {
		if (LogisticsSecurityTileEntity.byPassed.contains(entityplayer)) {
			return LogisticsSecurityTileEntity.allowAll;
		}
		if (usePower && !useEnergy(10)) {
			entityplayer.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return new SecuritySettings("No Energy");
		}
		SecuritySettings setting = settingsList.get(entityplayer.getName().getString());
		//TODO Change to GameProfile based Authentication
		if (setting == null) {
			setting = new SecuritySettings(entityplayer.getName().getString());
			settingsList.put(entityplayer.getName().getString(), setting);
		}
		return setting;
	}

	public void changeCC() {
		allowCC = !allowCC;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationCC.class).putInt(allowCC ? 1 : 0).setBlockPos(getBlockPos()), listener);
	}

	public void changeDestroy() {
		allowAutoDestroy = !allowAutoDestroy;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationAutoDestroy.class).putInt(allowAutoDestroy ? 1 : 0).setBlockPos(getBlockPos()), listener);
	}

	public void addCCToList(Integer id) {
		if (!excludedCC.contains(id)) {
			excludedCC.add(id);
		}
		Collections.sort(excludedCC);
	}

	public void removeCCFromList(Integer id) {
		excludedCC.remove(id);
	}

	public void requestList(Player player) {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		for (Integer i : excludedCC) {
			list.add(IntTag.valueOf(i));
		}
		tag.put("list", list);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCCIDs.class).put(tag).setBlockPos(getBlockPos()), player);
	}

	public void handleListPacket(CompoundTag tag) {
		excludedCC.clear();
		ListTag list = tag.getListOrEmpty("list");
		while (list.size() > 0) {
			Tag base = list.remove(0);
			excludedCC.add(((IntTag) base).intValue());
		}
	}

	@Override
	public boolean getAllowCC(int id) {
		if (!useEnergy(10)) {
			return false;
		}
		return allowCC != excludedCC.contains(id);
	}

	@Override
	public boolean canAutomatedDestroy() {
		if (!useEnergy(10)) {
			return false;
		}
		return allowAutoDestroy;
	}

	private boolean useEnergy(int amount) {
		for (int i = 0; i < 4; i++) {
			BlockEntity tile = getWorld().getBlockEntity(getBlockPos().relative(Direction.values()[i + 2]));
			if (tile instanceof IRoutedPowerProvider) {
				if (((IRoutedPowerProvider) tile).useEnergy(amount)) {
					return true;
				}
			}
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof IRoutedPowerProvider) {
					if (((IRoutedPowerProvider) ((LogisticsTileGenericPipe) tile).pipe).useEnergy(amount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(SecurityStationGui.class);
	}
}
