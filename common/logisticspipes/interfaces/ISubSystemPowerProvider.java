package logisticspipes.interfaces;

public interface ISubSystemPowerProvider {

	int getPowerLevel();

	void requestPower(int destination, int amount);

	boolean usePaused();
}
