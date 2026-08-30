package logisticspipes.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class Integer2ModuleCoordinatesPacket extends IntegerModuleCoordinatesPacket {

	@Getter
	@Setter
	private int integer2;

	public Integer2ModuleCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setInteger2(input.readInt());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(getInteger2());
	}
}
