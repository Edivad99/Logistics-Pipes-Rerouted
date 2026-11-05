package logisticspipes.interfaces;

/**
 * A power provider that sends power to the pipes that ask for it.
 */
public interface ISubSystemPowerProvider {

    int getPowerLevel();

    /**
     * @param destination the router id of the pipe to send the power to
     */
    void requestPower(int destination, int amount);

    /**
     * @return whether it has stopped taking requests for now
     */
    boolean usePaused();
}
