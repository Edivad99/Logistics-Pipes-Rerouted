package logisticspipes.network.abstractguis;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class NBTModuleCoordinatesGuiProvider extends ModuleCoordinatesGuiProvider {

	@Getter
	@Setter
	private CompoundTag nbt;

	public NBTModuleCoordinatesGuiProvider(int id) {
		super(id);
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
