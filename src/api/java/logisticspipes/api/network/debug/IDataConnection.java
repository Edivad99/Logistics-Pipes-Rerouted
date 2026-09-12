package logisticspipes.api.network.debug;

public interface IDataConnection {

	void passData(byte[] packet);

	void closeCon();
}
