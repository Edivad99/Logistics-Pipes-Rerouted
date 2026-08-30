package logisticspipes.network.packetcontent;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public interface IPacketContent<T> {

	T getValue();

	void setValue(T value);

	void readData(LPDataInput input);

	void writeData(LPDataOutput output);
}
