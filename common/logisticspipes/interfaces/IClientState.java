package logisticspipes.interfaces;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public interface IClientState {

	void writeData(LPDataOutput output);

	void readData(LPDataInput input);
}
