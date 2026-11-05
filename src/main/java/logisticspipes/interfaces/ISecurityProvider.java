package logisticspipes.interfaces;

/**
 * A security station, which decides what automation may do to the pipes it guards. Asking costs it
 * power, and without power the answer is no.
 */
public interface ISecurityProvider {

    /**
     * @return whether the computer may use the pipe
     */
    boolean getAllowCC(int computerId);

    boolean canAutomatedDestroy();
}
