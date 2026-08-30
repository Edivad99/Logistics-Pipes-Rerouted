package logisticspipes.network;

import logisticspipes.util.LPDataOutput;

public interface IWriteListObject<T> {

	void writeObject(LPDataOutput output, T object);
}
