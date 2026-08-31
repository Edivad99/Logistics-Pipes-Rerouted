package logisticspipes.network.abstractpackets;

import net.minecraft.nbt.CompoundTag;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class NBTCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private @Nullable CompoundTag tag;

	public NBTCoordinatesPacket(int id) {
		super(id);
	}

	public NBTCoordinatesPacket put(CompoundTag value) {
		setTag(value);
		return this;
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
