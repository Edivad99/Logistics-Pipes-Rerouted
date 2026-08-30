package logisticspipes.network.packets.modules;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.inventory.ProviderMode;

@StaticResolve
public class ProviderModuleMode extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int mode;

	public ProviderModuleMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ProviderModuleMode(getId());
	}

	@Override
	public void processPacket(Player player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.providerMode.setValue(ProviderMode.modeFromIntSafe(mode));
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(mode);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		mode = input.readInt();
	}
}
