package logisticspipes.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class IntegerPacket extends ModernPacket {

	@Getter
	@Setter
	private int integer;

	public IntegerPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setInteger(input.readInt());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(getInteger());
	}
}
