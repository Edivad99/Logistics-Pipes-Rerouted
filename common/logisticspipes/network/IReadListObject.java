package logisticspipes.network;

import logisticspipes.util.LPDataInput;

public interface IReadListObject<T> {

	T readObject(LPDataInput input);
}
