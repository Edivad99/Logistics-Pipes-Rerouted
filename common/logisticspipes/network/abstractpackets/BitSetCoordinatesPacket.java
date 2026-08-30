package logisticspipes.network.abstractpackets;

import java.util.BitSet;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class BitSetCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private BitSet flags;

	public BitSetCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBitSet(getFlags());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setFlags(input.readBitSet());
	}
}
