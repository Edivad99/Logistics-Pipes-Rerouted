package logisticspipes.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class StringCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private String string;

	public StringCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeUTF(getString());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setString(input.readUTF());
	}
}
