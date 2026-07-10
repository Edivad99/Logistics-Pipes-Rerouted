package logisticspipes.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class NBTModuleCoordinatesPacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private CompoundTag tag;

	public NBTModuleCoordinatesPacket(int id) {
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
		tag = input.readCompoundTag();
	}
}
