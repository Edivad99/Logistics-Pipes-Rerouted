package logisticspipes.network.packets.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ModuleBasedItemSinkList extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private CompoundTag nbt;

	public ModuleBasedItemSinkList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ModuleBasedItemSinkList(getId());
	}

	@Override
	public void processPacket(Player player) {
		IStringBasedModule module = this.getLogisticsModule(player, IStringBasedModule.class);
		if (module == null) {
			return;
		}
		module.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), nbt));
		if (MainProxy.isServer(player.level()) && getType().isInWorld()) {
			module.listChanged();
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundTag(nbt);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		nbt = input.readCompoundTag();
	}
}
