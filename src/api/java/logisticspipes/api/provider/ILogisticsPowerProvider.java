package logisticspipes.api.provider;

/**
 * A block that puts power into the logistics network, rather than passing along someone else's.
 *
 * <p>The network caches which of these it can reach and draws from the nearest one that has power.
 */
public interface ILogisticsPowerProvider extends IRoutedPowerProvider {

    /** How much power is stored right now, for display and for choosing between providers. */
    int getPowerLevel();
}
