package logisticspipes.network.packets.module;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.Objects;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ModulePropertiesUpdate extends ModuleCoordinatesPacket {

	public CompoundTag tag = new CompoundTag();

	public ModulePropertiesUpdate(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundTag(tag);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		tag = Objects.requireNonNull(input.readCompoundTag(), "read null NBT in ModulePropertiesUpdate");
	}

	@Override
	public ModernPacket template() {
		return new ModulePropertiesUpdate(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsModule module = this.getLogisticsModule(player, LogisticsModule.class);
		if (module == null) {
			return;
		}

		// sync updated properties
		RegistryAccess registryAccess = player.level().registryAccess();
		module.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, tag));

		if (!getType().isInWorld() && player.containerMenu instanceof InventoryMenu) {
			// sync slot in player inventory and mark player inventory dirty
			ItemModuleInformationManager.saveInformation(player.getInventory().getItem(getPositionInt()), module, registryAccess);
			player.getInventory().setChanged();
		}

		MainProxy.runOnServer(player.level(), () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder(module, player.registryAccess()).setModulePos(module), player);
		});
	}

	public static ModuleCoordinatesPacket fromPropertyHolder(PropertyHolder holder, HolderLookup.Provider provider) {
		final ModulePropertiesUpdate packet = PacketHandler.getPacket(ModulePropertiesUpdate.class);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
		PropertyHolder.serialize(output, holder);
		packet.tag = output.buildResult();
		return packet;
	}

}
