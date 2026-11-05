package logisticspipes.interfaces;

/**
 * A power block whose level shows on its HUD.
 */
public interface IPowerLevelDisplay {

    /**
     * @return how full it is, as a percentage
     */
    int getChargeState();

    int getDisplayPowerLevel();

    int getMaxStorage();

    boolean isHUDInvalid();
}
